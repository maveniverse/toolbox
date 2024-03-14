/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal.resolver;

import static eu.maveniverse.maven.toolbox.shared.internal.BuildScopeQuery.all;
import static eu.maveniverse.maven.toolbox.shared.internal.BuildScopeQuery.byBuildPath;
import static eu.maveniverse.maven.toolbox.shared.internal.BuildScopeQuery.byProjectPath;
import static eu.maveniverse.maven.toolbox.shared.internal.BuildScopeQuery.select;
import static eu.maveniverse.maven.toolbox.shared.internal.BuildScopeQuery.singleton;
import static eu.maveniverse.maven.toolbox.shared.internal.BuildScopeQuery.union;
import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.toolbox.shared.BuildPath;
import eu.maveniverse.maven.toolbox.shared.DependencyScope;
import eu.maveniverse.maven.toolbox.shared.ProjectPath;
import eu.maveniverse.maven.toolbox.shared.ResolutionScope;
import eu.maveniverse.maven.toolbox.shared.internal.BuildScope;
import eu.maveniverse.maven.toolbox.shared.internal.BuildScopeMatrixSource;
import eu.maveniverse.maven.toolbox.shared.internal.BuildScopeQuery;
import eu.maveniverse.maven.toolbox.shared.internal.BuildScopeSource;
import eu.maveniverse.maven.toolbox.shared.internal.CommonBuilds;
import eu.maveniverse.maven.toolbox.shared.internal.InternalScopeManager;
import java.util.Arrays;
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
    public static final String NAME = "java";

    public enum MavenLevel {
        Maven2(
                true,
                true,
                true,
                false,
                false,
                false,
                new BuildScopeMatrixSource(
                        Collections.singletonList(CommonBuilds.PROJECT_PATH_MAIN),
                        Arrays.asList(CommonBuilds.BUILD_PATH_COMPILE, CommonBuilds.BUILD_PATH_RUNTIME),
                        CommonBuilds.MAVEN_TEST_BUILD_SCOPE)),
        Maven3(
                true,
                false,
                true,
                false,
                false,
                false,
                new BuildScopeMatrixSource(
                        Collections.singletonList(CommonBuilds.PROJECT_PATH_MAIN),
                        Arrays.asList(CommonBuilds.BUILD_PATH_COMPILE, CommonBuilds.BUILD_PATH_RUNTIME),
                        CommonBuilds.MAVEN_TEST_BUILD_SCOPE)),
        Maven4WithoutSystem(
                false,
                false,
                false,
                true,
                false,
                false,
                new BuildScopeMatrixSource(
                        Arrays.asList(CommonBuilds.PROJECT_PATH_MAIN, CommonBuilds.PROJECT_PATH_TEST),
                        Arrays.asList(CommonBuilds.BUILD_PATH_COMPILE, CommonBuilds.BUILD_PATH_RUNTIME))),
        Maven4WithSystem(
                true,
                false,
                false,
                true,
                false,
                false,
                new BuildScopeMatrixSource(
                        Arrays.asList(CommonBuilds.PROJECT_PATH_MAIN, CommonBuilds.PROJECT_PATH_TEST),
                        Arrays.asList(CommonBuilds.BUILD_PATH_COMPILE, CommonBuilds.BUILD_PATH_RUNTIME))),
        Maven4Full(
                true,
                false,
                false,
                true,
                true,
                true,
                new BuildScopeMatrixSource(
                        Arrays.asList(
                                CommonBuilds.PROJECT_PATH_MAIN,
                                CommonBuilds.PROJECT_PATH_TEST,
                                CommonBuilds.PROJECT_PATH_IT),
                        Arrays.asList(
                                CommonBuilds.BUILD_PATH_PREPROCESS,
                                CommonBuilds.BUILD_PATH_COMPILE,
                                CommonBuilds.BUILD_PATH_RUNTIME)));

        private final boolean systemScope;
        private final boolean systemScopeTransitive;
        private final boolean brokenRuntimeResolution;
        private final boolean newDependencyScopes;
        private final boolean strictDependencyScopes;
        private final boolean strictResolutionScopes;
        private final BuildScopeSource buildScopeSource;

        MavenLevel(
                boolean systemScope,
                boolean systemScopeTransitive,
                boolean brokenRuntimeResolution,
                boolean newDependencyScopes,
                boolean strictDependencyScopes,
                boolean strictResolutionScopes,
                BuildScopeSource buildScopeSource) {
            this.systemScope = systemScope;
            this.systemScopeTransitive = systemScopeTransitive;
            this.brokenRuntimeResolution = brokenRuntimeResolution;
            this.newDependencyScopes = newDependencyScopes;
            this.strictDependencyScopes = strictDependencyScopes;
            this.strictResolutionScopes = strictResolutionScopes;
            this.buildScopeSource = buildScopeSource;
        }

        public boolean isSystemScope() {
            return systemScope;
        }

        public boolean isSystemScopeTransitive() {
            return systemScopeTransitive;
        }

        public boolean isBrokenRuntimeResolution() {
            return brokenRuntimeResolution;
        }

        public boolean isNewDependencyScopes() {
            return newDependencyScopes;
        }

        public BuildScopeSource getBuildScopeSource() {
            return buildScopeSource;
        }
    }

    private static final String DS_NONE = "none";
    private static final String DS_COMPILE = "compile";
    private static final String DS_COMPILE_ONLY = "compileOnly";
    private static final String DS_RUNTIME = "runtime";
    private static final String DS_PROVIDED = "provided";
    private static final String DS_SYSTEM = "system";
    private static final String DS_TEST = "test";
    private static final String DS_TEST_RUNTIME = "testRuntime";
    private static final String DS_TEST_ONLY = "testOnly";
    private static final String RS_NONE = "none";
    private static final String RS_MAIN_COMPILE = "main-compile";
    private static final String RS_MAIN_COMPILE_PLUS_RUNTIME = "main-compilePlusRuntime";
    private static final String RS_MAIN_RUNTIME = "main-runtime";
    private static final String RS_MAIN_RUNTIME_PLUS_SYSTEM = "main-runtimePlusSystem";
    private static final String RS_TEST_COMPILE = "test-compile";
    private static final String RS_TEST_RUNTIME = "test-runtime";

    private final String id;
    private final MavenLevel mavenLevel;
    private final BuildScopeSource buildScopeSource;
    private final Map<String, DependencyScopeImpl> dependencyScopes;
    private final Map<String, ResolutionScopeImpl> resolutionScopes;

    public ScopeManagerImpl(MavenLevel mavenLevel) {
        this.id = NAME;
        this.mavenLevel = requireNonNull(mavenLevel, "mavenLevel");
        this.buildScopeSource = mavenLevel.getBuildScopeSource();
        this.dependencyScopes = Collections.unmodifiableMap(buildDependencyScopes());
        this.resolutionScopes = Collections.unmodifiableMap(buildResolutionScopes());
    }

    private Map<String, DependencyScopeImpl> buildDependencyScopes() {
        HashMap<String, DependencyScopeImpl> result = new HashMap<>();
        result.put(DS_COMPILE, new DependencyScopeImpl(DS_COMPILE, true, all()));
        result.put(DS_RUNTIME, new DependencyScopeImpl(DS_RUNTIME, true, byBuildPath(CommonBuilds.BUILD_PATH_RUNTIME)));
        result.put(
                DS_PROVIDED,
                new DependencyScopeImpl(
                        DS_PROVIDED,
                        false,
                        union(
                                byBuildPath(CommonBuilds.BUILD_PATH_COMPILE),
                                select(CommonBuilds.PROJECT_PATH_TEST, CommonBuilds.BUILD_PATH_RUNTIME))));
        result.put(DS_TEST, new DependencyScopeImpl(DS_TEST, false, byProjectPath(CommonBuilds.PROJECT_PATH_TEST)));
        if (mavenLevel.isSystemScope()) {
            result.put(DS_SYSTEM, new DependencyScopeImpl(DS_SYSTEM, mavenLevel.isSystemScopeTransitive(), all()));
        }
        if (mavenLevel.isNewDependencyScopes()) {
            result.put(DS_NONE, new DependencyScopeImpl(DS_NONE, false, Collections.emptySet()));
            result.put(
                    DS_COMPILE_ONLY,
                    new DependencyScopeImpl(
                            DS_COMPILE_ONLY,
                            false,
                            singleton(CommonBuilds.PROJECT_PATH_MAIN, CommonBuilds.BUILD_PATH_COMPILE)));
            result.put(
                    DS_TEST_RUNTIME,
                    new DependencyScopeImpl(
                            DS_TEST_RUNTIME,
                            false,
                            singleton(CommonBuilds.PROJECT_PATH_TEST, CommonBuilds.BUILD_PATH_RUNTIME)));
            result.put(
                    DS_TEST_ONLY,
                    new DependencyScopeImpl(
                            DS_TEST_ONLY,
                            false,
                            singleton(CommonBuilds.PROJECT_PATH_TEST, CommonBuilds.BUILD_PATH_COMPILE)));
        }
        return result;
    }

    private Map<String, ResolutionScopeImpl> buildResolutionScopes() {
        Collection<DependencyScope> allDependencyScopes = new HashSet<>(dependencyScopes.values());
        Collection<DependencyScope> nonTransitiveDependencyScopes = dependencyScopes.values().stream()
                .filter(s -> !s.isTransitive())
                .collect(Collectors.toSet());

        HashMap<String, ResolutionScopeImpl> result = new HashMap<>();
        result.put(
                RS_NONE,
                new ResolutionScopeImpl(
                        RS_NONE,
                        ResolutionScopeImpl.Mode.REMOVE,
                        Collections.emptySet(),
                        Collections.emptySet(),
                        allDependencyScopes));
        result.put(
                RS_MAIN_COMPILE,
                new ResolutionScopeImpl(
                        RS_MAIN_COMPILE,
                        ResolutionScopeImpl.Mode.ELIMINATE,
                        singleton(CommonBuilds.PROJECT_PATH_MAIN, CommonBuilds.BUILD_PATH_COMPILE),
                        Collections.singletonList(dependencyScopes.get(DS_SYSTEM)),
                        nonTransitiveDependencyScopes));
        result.put(
                RS_MAIN_COMPILE_PLUS_RUNTIME,
                new ResolutionScopeImpl(
                        RS_MAIN_COMPILE_PLUS_RUNTIME,
                        ResolutionScopeImpl.Mode.ELIMINATE,
                        byProjectPath(CommonBuilds.PROJECT_PATH_MAIN),
                        Collections.singletonList(dependencyScopes.get(DS_SYSTEM)),
                        nonTransitiveDependencyScopes));
        result.put(
                RS_MAIN_RUNTIME,
                new ResolutionScopeImpl(
                        RS_MAIN_RUNTIME,
                        mavenLevel.isBrokenRuntimeResolution()
                                ? ResolutionScopeImpl.Mode.ELIMINATE
                                : ResolutionScopeImpl.Mode.REMOVE,
                        singleton(CommonBuilds.PROJECT_PATH_MAIN, CommonBuilds.BUILD_PATH_RUNTIME),
                        Collections.emptySet(),
                        nonTransitiveDependencyScopes));
        if (mavenLevel.isSystemScope()) {
            result.put(
                    RS_MAIN_RUNTIME_PLUS_SYSTEM,
                    new ResolutionScopeImpl(
                            RS_MAIN_RUNTIME_PLUS_SYSTEM,
                            mavenLevel.isBrokenRuntimeResolution()
                                    ? ResolutionScopeImpl.Mode.ELIMINATE
                                    : ResolutionScopeImpl.Mode.REMOVE,
                            singleton(CommonBuilds.PROJECT_PATH_MAIN, CommonBuilds.BUILD_PATH_RUNTIME),
                            Collections.singletonList(dependencyScopes.get(DS_SYSTEM)),
                            nonTransitiveDependencyScopes));
        }
        result.put(
                RS_TEST_COMPILE,
                new ResolutionScopeImpl(
                        RS_TEST_COMPILE,
                        ResolutionScopeImpl.Mode.ELIMINATE,
                        select(CommonBuilds.PROJECT_PATH_TEST, CommonBuilds.BUILD_PATH_COMPILE),
                        Collections.singletonList(dependencyScopes.get(DS_SYSTEM)),
                        nonTransitiveDependencyScopes));
        result.put(
                RS_TEST_RUNTIME,
                new ResolutionScopeImpl(
                        RS_TEST_RUNTIME,
                        ResolutionScopeImpl.Mode.ELIMINATE,
                        select(CommonBuilds.PROJECT_PATH_TEST, CommonBuilds.BUILD_PATH_RUNTIME),
                        Collections.singletonList(dependencyScopes.get(DS_SYSTEM)),
                        nonTransitiveDependencyScopes));
        return result;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDescription() {
        return "Java (as in " + mavenLevel.name() + ")";
    }

    @Override
    public Optional<DependencyScope> getSystemScope() {
        return Optional.ofNullable(dependencyScopes.get(DS_SYSTEM));
    }

    @Override
    public Optional<DependencyScope> getDependencyScope(String id) {
        DependencyScope dependencyScope = dependencyScopes.get(id);
        if (mavenLevel.strictDependencyScopes && dependencyScope == null) {
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
        if (mavenLevel.strictResolutionScopes && resolutionScope == null) {
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
        int result = 0;
        if (dependencyScope.isTransitive()) {
            result += 1000;
        }
        for (BuildScope buildScope :
                buildScopeSource.query(translate(dependencyScope).getPresence())) {
            result += 1000
                    / buildScope.getProjectPaths().stream()
                            .map(ProjectPath::order)
                            .reduce(0, Integer::sum);
        }
        return result;
    }

    @Override
    public Optional<BuildScope> getDependencyScopeMainProjectBuildScope(DependencyScope dependencyScope) {
        for (ProjectPath projectPath : buildScopeSource.allProjectPaths().stream()
                .sorted(Comparator.comparing(ProjectPath::order))
                .toList()) {
            for (BuildPath buildPath : buildScopeSource.allBuildPaths().stream()
                    .sorted(Comparator.comparing(BuildPath::order))
                    .toList()) {
                for (BuildScope buildScope :
                        buildScopeSource.query(translate(dependencyScope).getPresence())) {
                    if (buildScope.getProjectPaths().contains(projectPath)
                            && buildScope.getBuildPaths().contains(buildPath)) {
                        return Optional.of(buildScope);
                    }
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public DependencySelector getDependencySelector(ResolutionScope resolutionScope) {
        ResolutionScopeImpl rs = translate(resolutionScope);
        Set<String> directlyExcludedLabels = getDirectlyExcludedLabels(rs);
        Set<String> transitivelyExcludedLabels = getTransitivelyExcludedLabels(rs);

        return new AndDependencySelector(
                rs.getMode() == ResolutionScopeImpl.Mode.ELIMINATE
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
        if (rs.getMode() == ResolutionScopeImpl.Mode.ELIMINATE) {
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

    private Set<DependencyScope> collectScopes(Collection<BuildScopeQuery> wantedPresence) {
        HashSet<DependencyScope> result = new HashSet<>();
        for (BuildScope buildScope : buildScopeSource.query(wantedPresence)) {
            dependencyScopes.values().stream()
                    .filter(s -> buildScopeSource.query(s.getPresence()).contains(buildScope))
                    .filter(s -> !s.getId().equals(DS_SYSTEM)) // system scope must be always explicitly added
                    .forEach(result::add);
        }
        return result;
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

    private static final class DependencyScopeImpl implements DependencyScope {
        private final String id;
        private final boolean transitive;
        private final Set<BuildScopeQuery> presence;

        private DependencyScopeImpl(String id, boolean transitive, Collection<BuildScopeQuery> presence) {
            this.id = requireNonNull(id, "id");
            this.transitive = transitive;
            this.presence = Set.copyOf(presence);
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
        /**
         * The mode of resolution scope: eliminate (remove all occurrences) or just remove.
         */
        enum Mode {
            /**
             * Mode where artifacts in non-wanted scopes are completely eliminated. In other words, this mode ensures
             * that if a dependency was removed due unwanted scope, it is guaranteed that no such dependency will appear
             * anywhere else in the resulting graph either.
             */
            ELIMINATE,

            /**
             * Mode where artifacts in non-wanted scopes are removed only. In other words, they will NOT prevent (as in
             * they will not "dominate") other possibly appearing occurrences of same artifact in the graph.
             */
            REMOVE
        }

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
            this.transitivelyExcluded =
                    transitivelyExcluded.stream().filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());
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
        ScopeManagerImpl scopeManager = new ScopeManagerImpl(MavenLevel.Maven4Full);
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
        System.out.println(getDescription() + " defined build scopes:");
        buildScopeSource.query(all()).stream()
                .sorted(Comparator.comparing(BuildScope::order))
                .forEach(s -> System.out.println(s.getId() + " (order=" + s.order() + ")"));
    }

    private void dumpDependencyScopes(ScopeManagerImpl scopeManager) {
        System.out.println(getDescription() + " defined dependency scopes:");
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
        System.out.println(getDescription() + " defined dependency derive matrix:");
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
        System.out.println(getDescription() + " defined resolution scopes:");
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
