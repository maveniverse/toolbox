/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal.resolver;

import static eu.maveniverse.maven.toolbox.shared.internal.BuildScopeQuery.all;
import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.toolbox.shared.BuildPath;
import eu.maveniverse.maven.toolbox.shared.DependencyScope;
import eu.maveniverse.maven.toolbox.shared.ProjectPath;
import eu.maveniverse.maven.toolbox.shared.ResolutionScope;
import eu.maveniverse.maven.toolbox.shared.internal.BuildScope;
import eu.maveniverse.maven.toolbox.shared.internal.BuildScopeQuery;
import eu.maveniverse.maven.toolbox.shared.internal.BuildScopeSource;
import eu.maveniverse.maven.toolbox.shared.internal.InternalScopeManager;
import eu.maveniverse.maven.toolbox.shared.internal.MavenConfiguration;
import eu.maveniverse.maven.toolbox.shared.internal.ScopeManagerConfiguration;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyGraphTransformer;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.util.filter.ScopeDependencyFilter;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;
import org.eclipse.aether.util.graph.transformer.ChainedDependencyGraphTransformer;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.eclipse.aether.util.graph.transformer.NearestVersionSelector;
import org.eclipse.aether.util.graph.transformer.SimpleOptionalitySelector;
import org.eclipse.aether.util.graph.visitor.CloningDependencyVisitor;
import org.eclipse.aether.util.graph.visitor.FilteringDependencyVisitor;

public final class ScopeManagerImpl implements InternalScopeManager {
    private final String id;
    private final boolean strictDependencyScopes;
    private final boolean strictResolutionScopes;
    private final BuildScopeSource buildScopeSource;
    private final String systemLabel;
    private final Map<String, DependencyScopeImpl> dependencyScopes;
    private final Map<String, ResolutionScopeImpl> resolutionScopes;

    public ScopeManagerImpl(ScopeManagerConfiguration configuration) {
        this.id = configuration.getId();
        this.strictDependencyScopes = configuration.isStrictDependencyScopes();
        this.strictResolutionScopes = configuration.isStrictResolutionScopes();
        this.buildScopeSource = configuration.getBuildScopeSource();
        this.systemLabel = configuration.getSystemDependencyScopeLabel().orElse(null);
        this.dependencyScopes = Collections.unmodifiableMap(buildDependencyScopes(configuration));
        this.resolutionScopes = Collections.unmodifiableMap(buildResolutionScopes(configuration));
    }

    private Map<String, DependencyScopeImpl> buildDependencyScopes(ScopeManagerConfiguration configuration) {
        Collection<DependencyScope> dependencyScopes = configuration.buildDependencyScopes(this);
        HashMap<String, DependencyScopeImpl> result = new HashMap<>(dependencyScopes.size());
        dependencyScopes.forEach(d -> result.put(d.getId(), (DependencyScopeImpl) d));
        return result;
    }

    private Map<String, ResolutionScopeImpl> buildResolutionScopes(ScopeManagerConfiguration configuration) {
        Collection<ResolutionScope> resolutionScopes = configuration.buildResolutionScopes(this);
        HashMap<String, ResolutionScopeImpl> result = new HashMap<>(resolutionScopes.size());
        resolutionScopes.forEach(r -> result.put(r.getId(), (ResolutionScopeImpl) r));
        return result;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Optional<DependencyScope> getSystemScope() {
        if (systemLabel != null) {
            return Optional.of(requireNonNull(
                    dependencyScopes.get(systemLabel),
                    "config specified label for system dependency scope but did not create it"));
        }
        return Optional.empty();
    }

    @Override
    public Optional<DependencyScope> getDependencyScope(String id) {
        DependencyScope dependencyScope = dependencyScopes.get(id);
        if (strictDependencyScopes && dependencyScope == null) {
            throw new IllegalArgumentException("unknown dependency scope");
        }
        return Optional.ofNullable(dependencyScope);
    }

    @Override
    public Collection<DependencyScope> getDependencyScopeUniverse() {
        return new HashSet<>(dependencyScopes.values());
    }

    @Override
    public Optional<ResolutionScope> getResolutionScope(String id) {
        ResolutionScope resolutionScope = resolutionScopes.get(id);
        if (strictResolutionScopes && resolutionScope == null) {
            throw new IllegalArgumentException("unknown resolution scope");
        }
        return Optional.ofNullable(resolutionScope);
    }

    @Override
    public Collection<ResolutionScope> getResolutionScopeUniverse() {
        return new HashSet<>(resolutionScopes.values());
    }

    @Override
    public int getDependencyScopeWidth(DependencyScope dependencyScope) {
        return translate(dependencyScope).getWidth();
    }

    @Override
    public Optional<BuildScope> getDependencyScopeMainProjectBuildScope(DependencyScope dependencyScope) {
        return Optional.ofNullable(translate(dependencyScope).getMainBuildScope());
    }

    @Override
    public DependencySelector getDependencySelector(ResolutionScope resolutionScope) {
        ResolutionScopeImpl rs = translate(resolutionScope);
        Set<String> directlyExcludedLabels = getDirectlyExcludedLabels(rs);
        Set<String> transitivelyExcludedLabels = getTransitivelyExcludedLabels(rs);

        return new AndDependencySelector(
                rs.getMode() == Mode.ELIMINATE
                        ? ScopeDependencySelector.fromTo(2, 2, null, directlyExcludedLabels)
                        : ScopeDependencySelector.fromTo(1, 2, null, directlyExcludedLabels),
                ScopeDependencySelector.from(2, null, transitivelyExcludedLabels),
                OptionalDependencySelector.fromDirect(),
                new ExclusionDependencySelector());
    }

    @Override
    public DependencyGraphTransformer getDependencyGraphTransformer(ResolutionScope resolutionScope) {
        return new ChainedDependencyGraphTransformer(
                new ConflictResolver(
                        new NearestVersionSelector(), new ManagedScopeSelector(this),
                        new SimpleOptionalitySelector(), new ManagedScopeDeriver(this)),
                new ManagedDependencyContextRefiner(this));
    }

    @Override
    public CollectResult postProcess(ResolutionScope resolutionScope, CollectResult collectResult) {
        ResolutionScopeImpl rs = translate(resolutionScope);
        if (rs.getMode() == Mode.ELIMINATE) {
            CloningDependencyVisitor cloning = new CloningDependencyVisitor();
            FilteringDependencyVisitor filter = new FilteringDependencyVisitor(
                    cloning, new ScopeDependencyFilter(null, getDirectlyExcludedLabels(rs)));
            collectResult.getRoot().accept(filter);
            collectResult.setRoot(cloning.getRootNode());
        }
        return collectResult;
    }

    @Override
    public DependencyFilter getDependencyFilter(ResolutionScope resolutionScope) {
        return new ScopeDependencyFilter(null, getDirectlyExcludedLabels(translate(resolutionScope)));
    }

    @Override
    public DependencyScope createDependencyScope(String id, boolean transitive, Collection<BuildScopeQuery> presence) {
        return new DependencyScopeImpl(id, transitive, presence);
    }

    @Override
    public ResolutionScope createResolutionScope(
            String id,
            Mode mode,
            Collection<BuildScopeQuery> wantedPresence,
            Collection<DependencyScope> explicitlyIncluded,
            Collection<DependencyScope> transitivelyExcluded) {
        return new ResolutionScopeImpl(id, mode, wantedPresence, explicitlyIncluded, transitivelyExcluded);
    }

    private Set<DependencyScope> collectScopes(Collection<BuildScopeQuery> wantedPresence) {
        HashSet<DependencyScope> result = new HashSet<>();
        for (BuildScope buildScope : buildScopeSource.query(wantedPresence)) {
            dependencyScopes.values().stream()
                    .filter(s -> buildScopeSource.query(s.getPresence()).contains(buildScope))
                    .filter(s -> !s.getId().equals(systemLabel)) // system scope must be always explicitly added
                    .forEach(result::add);
        }
        return result;
    }

    private int calculateDependencyScopeWidth(DependencyScopeImpl dependencyScope) {
        int result = 0;
        if (dependencyScope.isTransitive()) {
            result += 1000;
        }
        for (BuildScope buildScope : buildScopeSource.query(dependencyScope.getPresence())) {
            result += 1000
                    / buildScope.getProjectPaths().stream()
                            .map(ProjectPath::order)
                            .reduce(0, Integer::sum);
        }
        return result;
    }

    private BuildScope calculateMainProjectBuildScope(DependencyScopeImpl dependencyScope) {
        for (ProjectPath projectPath : buildScopeSource.allProjectPaths().stream()
                .sorted(Comparator.comparing(ProjectPath::order))
                .collect(Collectors.toList())) {
            for (BuildPath buildPath : buildScopeSource.allBuildPaths().stream()
                    .sorted(Comparator.comparing(BuildPath::order))
                    .collect(Collectors.toList())) {
                for (BuildScope buildScope : buildScopeSource.query(dependencyScope.getPresence())) {
                    if (buildScope.getProjectPaths().contains(projectPath)
                            && buildScope.getBuildPaths().contains(buildPath)) {
                        return buildScope;
                    }
                }
            }
        }
        return null;
    }

    private Set<String> getDirectlyIncludedLabels(ResolutionScopeImpl resolutionScope) {
        return resolutionScope.getDirectlyIncluded().stream()
                .map(DependencyScope::getId)
                .collect(Collectors.toSet());
    }

    private Set<String> getDirectlyExcludedLabels(ResolutionScopeImpl resolutionScope) {
        return dependencyScopes.values().stream()
                .filter(s -> !resolutionScope.getDirectlyIncluded().contains(s))
                .map(DependencyScope::getId)
                .collect(Collectors.toSet());
    }

    private Set<String> getTransitivelyExcludedLabels(ResolutionScopeImpl resolutionScope) {
        return resolutionScope.getTransitivelyExcluded().stream()
                .map(DependencyScope::getId)
                .collect(Collectors.toSet());
    }

    private DependencyScopeImpl translate(DependencyScope dependencyScope) {
        return requireNonNull(dependencyScopes.get(dependencyScope.getId()), "unknown dependency scope");
    }

    private ResolutionScopeImpl translate(ResolutionScope resolutionScope) {
        return requireNonNull(resolutionScopes.get(resolutionScope.getId()), "unknown resolution scope");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScopeManagerImpl that = (ScopeManagerImpl) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return id;
    }

    private final class DependencyScopeImpl implements DependencyScope {
        private final String id;
        private final boolean transitive;
        private final Set<BuildScopeQuery> presence;
        private final BuildScope mainBuildScope;
        private final int width;

        private DependencyScopeImpl(String id, boolean transitive, Collection<BuildScopeQuery> presence) {
            this.id = requireNonNull(id, "id");
            this.transitive = transitive;
            this.presence = Collections.unmodifiableSet(new HashSet<>(presence));
            this.mainBuildScope = calculateMainProjectBuildScope(this);
            this.width = calculateDependencyScopeWidth(this);
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public boolean isTransitive() {
            return transitive;
        }

        public Set<BuildScopeQuery> getPresence() {
            return presence;
        }

        public BuildScope getMainBuildScope() {
            return mainBuildScope;
        }

        public int getWidth() {
            return width;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DependencyScopeImpl that = (DependencyScopeImpl) o;
            return Objects.equals(id, that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return id;
        }
    }

    private final class ResolutionScopeImpl implements ResolutionScope {

        private final String id;
        private final Mode mode;
        private final Set<BuildScopeQuery> wantedPresence;
        private final Set<DependencyScope> directlyIncluded;
        private final Set<DependencyScope> transitivelyExcluded;

        private ResolutionScopeImpl(
                String id,
                Mode mode,
                Collection<BuildScopeQuery> wantedPresence,
                Collection<DependencyScope> explicitlyIncluded,
                Collection<DependencyScope> transitivelyExcluded) {
            this.id = requireNonNull(id, "id");
            this.mode = requireNonNull(mode, "mode");
            this.wantedPresence = Collections.unmodifiableSet(new HashSet<>(wantedPresence));
            Set<DependencyScope> included = collectScopes(wantedPresence);
            // here we may have null elements, based on existence of system scope
            if (explicitlyIncluded != null && !explicitlyIncluded.isEmpty()) {
                explicitlyIncluded.stream().filter(Objects::nonNull).forEach(included::add);
            }
            this.directlyIncluded = Collections.unmodifiableSet(included);
            this.transitivelyExcluded = Collections.unmodifiableSet(
                    transitivelyExcluded.stream().filter(Objects::nonNull).collect(Collectors.toSet()));
        }

        @Override
        public String getId() {
            return id;
        }

        public Mode getMode() {
            return mode;
        }

        public Set<BuildScopeQuery> getWantedPresence() {
            return wantedPresence;
        }

        public Set<DependencyScope> getDirectlyIncluded() {
            return directlyIncluded;
        }

        public Set<DependencyScope> getTransitivelyExcluded() {
            return transitivelyExcluded;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ResolutionScopeImpl that = (ResolutionScopeImpl) o;
            return Objects.equals(id, that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return id;
        }
    }

    public static void main(String... args) {
        ScopeManagerImpl scopeManager = new ScopeManagerImpl(MavenConfiguration.MAVEN3);
        System.out.println();
        scopeManager.dumpBuildScopes(scopeManager);
        System.out.println();
        scopeManager.dumpDependencyScopes(scopeManager);
        System.out.println();
        scopeManager.dumpDependencyScopeDerives(scopeManager);
        System.out.println();
        scopeManager.dumpResolutionScopes(scopeManager);

        scopeManager = new ScopeManagerImpl(MavenConfiguration.MAVEN4);
        System.out.println();
        scopeManager.dumpBuildScopes(scopeManager);
        System.out.println();
        scopeManager.dumpDependencyScopes(scopeManager);
        System.out.println();
        scopeManager.dumpDependencyScopeDerives(scopeManager);
        System.out.println();
        scopeManager.dumpResolutionScopes(scopeManager);
    }

    private void dumpBuildScopes(ScopeManagerImpl scopeManager) {
        System.out.println(getId() + " defined build scopes:");
        buildScopeSource.query(all()).stream()
                .sorted(Comparator.comparing(BuildScope::order))
                .forEach(s -> System.out.println(s.getId() + " (order=" + s.order() + ")"));
    }

    private void dumpDependencyScopes(ScopeManagerImpl scopeManager) {
        System.out.println(getId() + " defined dependency scopes:");
        dependencyScopes.values().stream()
                .sorted(Comparator.comparing(scopeManager::getDependencyScopeWidth)
                        .reversed())
                .forEach(s -> {
                    System.out.println(s + " (width=" + scopeManager.getDependencyScopeWidth(s) + ")");
                    System.out.println("  Query : " + s.getPresence());
                    System.out.println("  Presence: "
                            + buildScopeSource.query(s.getPresence()).stream()
                                    .map(BuildScope::getId)
                                    .collect(Collectors.toSet()));
                    System.out.println("  Main project scope: "
                            + scopeManager
                                    .getDependencyScopeMainProjectBuildScope(s)
                                    .map(BuildScope::getId)
                                    .orElse("null"));
                });
    }

    private void dumpDependencyScopeDerives(ScopeManagerImpl scopeManager) {
        System.out.println(getId() + " defined dependency derive matrix:");
        ManagedScopeDeriver deriver = new ManagedScopeDeriver(this);
        dependencyScopes.values().stream()
                .sorted(Comparator.comparing(scopeManager::getDependencyScopeWidth)
                        .reversed())
                .forEach(parent -> dependencyScopes.values().stream()
                        .sorted(Comparator.comparing(scopeManager::getDependencyScopeWidth)
                                .reversed())
                        .forEach(child -> System.out.println(parent.getId() + " w/ child " + child.getId() + " -> "
                                + deriver.getDerivedScope(parent.getId(), child.getId()))));
    }

    private void dumpResolutionScopes(ScopeManagerImpl scopeManager) {
        System.out.println(getId() + " defined resolution scopes:");
        resolutionScopes.values().stream()
                .sorted(Comparator.comparing(ResolutionScope::getId))
                .forEach(s -> {
                    System.out.println("* " + s.getId());
                    System.out.println("     Directly included: " + getDirectlyIncludedLabels(s));
                    System.out.println("     Directly excluded: " + getDirectlyExcludedLabels(s));
                    System.out.println(" Transitively excluded: " + getTransitivelyExcludedLabels(s));
                });
    }
}
