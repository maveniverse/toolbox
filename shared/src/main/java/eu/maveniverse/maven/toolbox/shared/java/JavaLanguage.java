/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.java;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.toolbox.shared.*;
import eu.maveniverse.maven.toolbox.shared.internal.*;
import java.util.*;
import java.util.stream.Collectors;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyGraphTransformer;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.util.filter.ScopeDependencyFilter;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;
import org.eclipse.aether.util.graph.transformer.ChainedDependencyGraphTransformer;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.eclipse.aether.util.graph.transformer.NearestVersionSelector;
import org.eclipse.aether.util.graph.transformer.SimpleOptionalitySelector;
import org.eclipse.aether.util.graph.visitor.CloningDependencyVisitor;
import org.eclipse.aether.util.graph.visitor.FilteringDependencyVisitor;

public final class JavaLanguage implements Language {
    public static final String NAME = "java";

    // TODO: maven2 and maven3 does not distinguish test-compile and test-runtime, they have "test" for both.
    public enum MavenLevel {
        Maven2(
                true,
                true,
                true,
                false,
                new BuildScopeMatrix(
                        Collections.singletonList(CommonBuilds.PROJECT_PATH_MAIN),
                        Arrays.asList(CommonBuilds.BUILD_PATH_COMPILE, CommonBuilds.BUILD_PATH_RUNTIME),
                        CommonBuilds.MAVEN_TEST_BUILD_SCOPE)),
        Maven3(
                true,
                false,
                true,
                false,
                new BuildScopeMatrix(
                        Collections.singletonList(CommonBuilds.PROJECT_PATH_MAIN),
                        Arrays.asList(CommonBuilds.BUILD_PATH_COMPILE, CommonBuilds.BUILD_PATH_RUNTIME),
                        CommonBuilds.MAVEN_TEST_BUILD_SCOPE)),
        Maven4WithoutSystem(
                false,
                false,
                false,
                true,
                new BuildScopeMatrix(
                        Arrays.asList(CommonBuilds.PROJECT_PATH_MAIN, CommonBuilds.PROJECT_PATH_TEST),
                        Arrays.asList(CommonBuilds.BUILD_PATH_COMPILE, CommonBuilds.BUILD_PATH_RUNTIME))),
        Maven4WithSystem(
                true,
                        false,
                        false,
                        true,
                        new BuildScopeMatrix(
                        Arrays.asList(CommonBuilds.PROJECT_PATH_MAIN, CommonBuilds.PROJECT_PATH_TEST),
                        Arrays.asList(CommonBuilds.BUILD_PATH_COMPILE, CommonBuilds.BUILD_PATH_RUNTIME)));

        private final boolean systemScope;

        private final boolean systemScopeTransitive;

        private final boolean brokenRuntimeResolution;

        private final boolean newDependencyScopes;

        private final BuildScopeMatrix buildScopeMatrix;

        MavenLevel(
                boolean systemScope,
                boolean systemScopeTransitive,
                boolean brokenRuntimeResolution,
                boolean newDependencyScopes,
                BuildScopeMatrix buildScopeMatrix) {
            this.systemScope = systemScope;
            this.systemScopeTransitive = systemScopeTransitive;
            this.brokenRuntimeResolution = brokenRuntimeResolution;
            this.newDependencyScopes = newDependencyScopes;
            this.buildScopeMatrix = buildScopeMatrix;
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

        public BuildScopeMatrix getBuildScopeMatrix() {
            return buildScopeMatrix;
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
    private final BuildScopeMatrix buildScopeMatrix;
    private final Map<String, JavaDependencyScope> dependencyScopes;
    private final Map<String, JavaResolutionScope> resolutionScopes;

    public JavaLanguage(MavenLevel mavenLevel) {
        this.id = NAME;
        this.mavenLevel = requireNonNull(mavenLevel, "mavenLevel");
        this.buildScopeMatrix = mavenLevel.getBuildScopeMatrix();
        this.dependencyScopes = Collections.unmodifiableMap(buildDependencyScopes());
        this.resolutionScopes = Collections.unmodifiableMap(buildResolutionScopes());
    }

    private Map<String, JavaDependencyScope> buildDependencyScopes() {
        HashMap<String, JavaDependencyScope> result = new HashMap<>();
        result.put(DS_COMPILE, new JavaDependencyScope(DS_COMPILE, this, true, buildScopeMatrix.all()));
        result.put(
                DS_RUNTIME,
                new JavaDependencyScope(
                        DS_RUNTIME, this, true, buildScopeMatrix.byBuildPath(CommonBuilds.BUILD_PATH_RUNTIME)));
        result.put(
                DS_PROVIDED,
                new JavaDependencyScope(
                        DS_PROVIDED,
                        this,
                        false,
                        buildScopeMatrix.union(
                                buildScopeMatrix.byBuildPath(CommonBuilds.BUILD_PATH_COMPILE),
                                buildScopeMatrix.select(
                                        CommonBuilds.PROJECT_PATH_TEST, CommonBuilds.BUILD_PATH_RUNTIME))));
        result.put(
                DS_TEST,
                new JavaDependencyScope(
                        DS_TEST, this, false, buildScopeMatrix.byProjectPath(CommonBuilds.PROJECT_PATH_TEST)));
        if (mavenLevel.isSystemScope()) {
            result.put(
                    DS_SYSTEM,
                    new JavaDependencyScope(
                            DS_SYSTEM, this, mavenLevel.isSystemScopeTransitive(), buildScopeMatrix.all()));
        }
        if (mavenLevel.isNewDependencyScopes()) {
            result.put(DS_NONE, new JavaDependencyScope(DS_NONE, this, false, Collections.emptySet()));
            result.put(
                    DS_COMPILE_ONLY,
                    new JavaDependencyScope(
                            DS_COMPILE_ONLY,
                            this,
                            false,
                            buildScopeMatrix.singleton(
                                    CommonBuilds.PROJECT_PATH_MAIN, CommonBuilds.BUILD_PATH_COMPILE)));
            result.put(
                    DS_TEST_RUNTIME,
                    new JavaDependencyScope(
                            DS_TEST_RUNTIME,
                            this,
                            false,
                            buildScopeMatrix.singleton(
                                    CommonBuilds.PROJECT_PATH_TEST, CommonBuilds.BUILD_PATH_RUNTIME)));
            result.put(
                    DS_TEST_ONLY,
                    new JavaDependencyScope(
                            DS_TEST_ONLY,
                            this,
                            false,
                            buildScopeMatrix.singleton(
                                    CommonBuilds.PROJECT_PATH_TEST, CommonBuilds.BUILD_PATH_COMPILE)));
        }
        return result;
    }

    private Set<JavaDependencyScope> collectScopes(Collection<BuildScope> buildScopes) {
        HashSet<JavaDependencyScope> result = new HashSet<>();
        for (BuildScope buildScope : buildScopes) {
            dependencyScopes.values().stream()
                    .filter(s -> s.getPresence().contains(buildScope))
                    .filter(s -> !s.getId().equals(DS_SYSTEM)) // system scope must be always explicitly added
                    .forEach(result::add);
        }
        return result;
    }

    private Map<String, JavaResolutionScope> buildResolutionScopes() {
        Collection<JavaDependencyScope> nonTransitiveScopes = dependencyScopes.values().stream()
                .filter(s -> !s.isTransitive())
                .collect(Collectors.toSet());

        HashMap<String, JavaResolutionScope> result = new HashMap<>();
        result.put(
                RS_NONE,
                new JavaResolutionScope(
                        RS_NONE,
                        this,
                        ResolutionScope.Mode.REMOVE,
                        Collections.emptySet(),
                        Collections.emptySet(),
                        dependencyScopes.values()));
        result.put(
                RS_MAIN_COMPILE,
                new JavaResolutionScope(
                        RS_MAIN_COMPILE,
                        this,
                        ResolutionScope.Mode.ELIMINATE,
                        buildScopeMatrix.singleton(CommonBuilds.PROJECT_PATH_MAIN, CommonBuilds.BUILD_PATH_COMPILE),
                        Collections.singletonList(dependencyScopes.get(DS_SYSTEM)),
                        nonTransitiveScopes));
        result.put(
                RS_MAIN_COMPILE_PLUS_RUNTIME,
                new JavaResolutionScope(
                        RS_MAIN_COMPILE_PLUS_RUNTIME,
                        this,
                        ResolutionScope.Mode.ELIMINATE,
                        buildScopeMatrix.byProjectPath(CommonBuilds.PROJECT_PATH_MAIN),
                        Collections.singletonList(dependencyScopes.get(DS_SYSTEM)),
                        nonTransitiveScopes));
        result.put(
                RS_MAIN_RUNTIME,
                new JavaResolutionScope(
                        RS_MAIN_RUNTIME,
                        this,
                        mavenLevel.isBrokenRuntimeResolution()
                                ? ResolutionScope.Mode.ELIMINATE
                                : ResolutionScope.Mode.REMOVE,
                        buildScopeMatrix.singleton(CommonBuilds.PROJECT_PATH_MAIN, CommonBuilds.BUILD_PATH_RUNTIME),
                        Collections.emptySet(),
                        nonTransitiveScopes));
        if (mavenLevel.isSystemScope()) {
            result.put(
                    RS_MAIN_RUNTIME_PLUS_SYSTEM,
                    new JavaResolutionScope(
                            RS_MAIN_RUNTIME_PLUS_SYSTEM,
                            this,
                            mavenLevel.isBrokenRuntimeResolution()
                                    ? ResolutionScope.Mode.ELIMINATE
                                    : ResolutionScope.Mode.REMOVE,
                            buildScopeMatrix.singleton(CommonBuilds.PROJECT_PATH_MAIN, CommonBuilds.BUILD_PATH_RUNTIME),
                            Collections.singletonList(dependencyScopes.get(DS_SYSTEM)),
                            nonTransitiveScopes));
        }
        result.put(
                RS_TEST_COMPILE,
                new JavaResolutionScope(
                        RS_TEST_COMPILE,
                        this,
                        ResolutionScope.Mode.ELIMINATE,
                        buildScopeMatrix.select(CommonBuilds.PROJECT_PATH_TEST, CommonBuilds.BUILD_PATH_COMPILE),
                        Collections.singletonList(dependencyScopes.get(DS_SYSTEM)),
                        nonTransitiveScopes));
        result.put(
                RS_TEST_RUNTIME,
                new JavaResolutionScope(
                        RS_TEST_RUNTIME,
                        this,
                        ResolutionScope.Mode.ELIMINATE,
                        buildScopeMatrix.select(CommonBuilds.PROJECT_PATH_TEST, CommonBuilds.BUILD_PATH_RUNTIME),
                        Collections.singletonList(dependencyScopes.get(DS_SYSTEM)),
                        nonTransitiveScopes));
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
        return Optional.ofNullable(dependencyScopes.get(id));
    }

    @Override
    public Collection<DependencyScope> getDependencyScopeUniverse() {
        return new HashSet<>(dependencyScopes.values());
    }

    @Override
    public Optional<ResolutionScope> getResolutionScope(String id) {
        return Optional.ofNullable(resolutionScopes.get(id));
    }

    @Override
    public Collection<ResolutionScope> getResolutionScopeUniverse() {
        return new HashSet<>(resolutionScopes.values());
    }

    private Optional<DependencyScope> deriveScope(DependencyScope parentScope, DependencyScope childScope) {
        JavaDependencyScope parent = parentScope != null ? validateAndCast(parentScope) : null;
        JavaDependencyScope child = validateAndCast(childScope);

        if ((DS_SYSTEM.equals(childScope.getId()) || DS_TEST.equals(childScope.getId()))) {
            return Optional.of(child);
        } else if (parent == null || DS_COMPILE.equals(parentScope.getId())) {
            return Optional.of(child);
        } else if (DS_TEST.equals(parentScope.getId()) || DS_RUNTIME.equals(parentScope.getId())) {
            return Optional.of(parent);
        } else if (DS_SYSTEM.equals(parentScope.getId()) || DS_PROVIDED.equals(parentScope.getId())) {
            return getDependencyScope(DS_PROVIDED);
        } else {
            return getDependencyScope(DS_RUNTIME);
        }
    }

    private Optional<BuildScope> getMainProjectBuildScope(JavaDependencyScope javaDependencyScope) {
        for (ProjectPath projectPath : buildScopeMatrix.allProjectPaths().stream()
                .sorted(Comparator.comparing(ProjectPath::order))
                .collect(Collectors.toList())) {
            for (BuildPath buildPath : buildScopeMatrix.allBuildPaths().stream()
                    .sorted(Comparator.comparing(BuildPath::order))
                    .collect(Collectors.toList())) {
                for (BuildScope buildScope : javaDependencyScope.getPresence()) {
                    if (buildScope.getProjectPaths().contains(projectPath)
                            && buildScope.getBuildPaths().contains(buildPath)) {
                        return Optional.of(buildScope);
                    }
                }
            }
        }
        return Optional.empty();
    }

    private Set<String> getDirectlyIncludedLabels(JavaResolutionScope javaResolutionScope) {
        return javaResolutionScope.getDirectlyIncluded().stream()
                .map(DependencyScope::getId)
                .collect(Collectors.toSet());
    }

    private Set<String> getDirectlyExcludedLabels(JavaResolutionScope javaResolutionScope) {
        return dependencyScopes.values().stream()
                .filter(s -> !javaResolutionScope.getDirectlyIncluded().contains(s))
                .map(DependencyScope::getId)
                .collect(Collectors.toSet());
    }

    private Set<String> getTransitivelyExcludedLabels(JavaResolutionScope javaResolutionScope) {
        return javaResolutionScope.getTransitivelyExcluded().stream()
                .map(DependencyScope::getId)
                .collect(Collectors.toSet());
    }

    private JavaDependencyScope validateAndCast(DependencyScope dependencyScope) {
        if (!(dependencyScope instanceof JavaDependencyScope) || dependencyScope.getLanguage() != this) {
            throw new IllegalArgumentException("unsupported resolution scope");
        }
        return (JavaDependencyScope) dependencyScope;
    }

    private JavaResolutionScope validateAndCast(ResolutionScope resolutionScope) {
        if (!(resolutionScope instanceof JavaResolutionScope) || resolutionScope.getLanguage() != this) {
            throw new IllegalArgumentException("unsupported resolution scope");
        }
        return (JavaResolutionScope) resolutionScope;
    }

    private DependencySelector getDependencySelector(ResolutionScope resolutionScope) {
        JavaResolutionScope javaResolutionScope = validateAndCast(resolutionScope);
        Set<String> directlyExcludedLabels = getDirectlyExcludedLabels(javaResolutionScope);
        Set<String> transitivelyExcludedLabels = getTransitivelyExcludedLabels(javaResolutionScope);

        return new AndDependencySelector(
                javaResolutionScope.getMode() == ResolutionScope.Mode.ELIMINATE
                        ? ScopeDependencySelector.fromTo(2, 2, null, directlyExcludedLabels)
                        : ScopeDependencySelector.fromTo(1, 2, null, directlyExcludedLabels),
                ScopeDependencySelector.from(2, null, transitivelyExcludedLabels),
                OptionalDependencySelector.fromDirect(),
                new ExclusionDependencySelector());
    }

    private DependencyGraphTransformer getDependencyGraphTransformer(ResolutionScope resolutionScope) {
        validateAndCast(resolutionScope);
        return new ChainedDependencyGraphTransformer(
                new ConflictResolver(
                        new NearestVersionSelector(), new LanguageScopeSelector(this),
                        new SimpleOptionalitySelector(), new LanguageScopeDeriver(this)),
                new LanguageDependencyContextRefiner(this));
    }

    private CollectResult postProcess(ResolutionScope resolutionScope, CollectResult collectResult) {
        JavaResolutionScope javaResolutionScope = validateAndCast(resolutionScope);
        if (javaResolutionScope.getMode() == ResolutionScope.Mode.ELIMINATE) {
            CloningDependencyVisitor cloning = new CloningDependencyVisitor();
            FilteringDependencyVisitor filter = new FilteringDependencyVisitor(
                    cloning, new ScopeDependencyFilter(null, getDirectlyExcludedLabels(javaResolutionScope)));
            collectResult.getRoot().accept(filter);
            collectResult.setRoot(cloning.getRootNode());
        }
        return collectResult;
    }

    private static int calculateWidth(JavaDependencyScope dependencyScope) {
        HashSet<ProjectPath> projectPaths = new HashSet<>();
        HashSet<BuildPath> buildPaths = new HashSet<>();
        dependencyScope.getPresence().forEach(s -> {
            projectPaths.addAll(s.getProjectPaths());
            buildPaths.addAll(s.getBuildPaths());
        });
        int result = 0;
        if (dependencyScope.isTransitive()) {
            result += 1000;
        }
        for (ProjectPath projectPath : projectPaths) {
            result += 1000 / projectPath.order();
        }
        result += buildPaths.size();
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JavaLanguage that = (JavaLanguage) o;
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

    public static final class JavaDependencyScope implements DependencyScope {
        private final String id;
        private final JavaLanguage javaLanguage;
        private final boolean transitive;
        private final Set<BuildScope> presence;
        private final int width;

        public JavaDependencyScope(
                String id, JavaLanguage javaLanguage, boolean transitive, Collection<BuildScope> presence) {
            this.id = requireNonNull(id, "id");
            this.javaLanguage = requireNonNull(javaLanguage, "javaLanguage");
            this.transitive = transitive;
            this.presence = Collections.unmodifiableSet(new HashSet<>(presence));
            this.width = calculateWidth(this);
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public JavaLanguage getLanguage() {
            return javaLanguage;
        }

        @Override
        public boolean isTransitive() {
            return transitive;
        }

        @Override
        public Set<BuildScope> getPresence() {
            return presence;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            JavaDependencyScope that = (JavaDependencyScope) o;
            return Objects.equals(id, that.id) && Objects.equals(javaLanguage, that.javaLanguage);
        }

        @Override
        public int width() {
            return width;
        }

        @Override
        public Optional<DependencyScope> deriveFromParent(DependencyScope parent) {
            return javaLanguage.deriveScope(parent, this);
        }

        @Override
        public Optional<BuildScope> getMainProjectBuildScope() {
            return javaLanguage.getMainProjectBuildScope(this);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, javaLanguage);
        }

        @Override
        public String toString() {
            return getLanguage().getId() + ":" + id;
        }
    }

    public final class JavaResolutionScope implements ResolutionScope {
        private final String id;
        private final JavaLanguage javaLanguage;
        private final Mode mode;
        private final Set<BuildScope> wantedPresence;
        private final Set<JavaDependencyScope> directlyIncluded;
        private final Set<JavaDependencyScope> transitivelyExcluded;

        private JavaResolutionScope(
                String id,
                JavaLanguage javaLanguage,
                Mode mode,
                Collection<BuildScope> wantedPresence,
                Collection<JavaDependencyScope> explicitlyIncluded,
                Collection<JavaDependencyScope> transitivelyExcluded) {
            this.id = requireNonNull(id, "id");
            this.javaLanguage = requireNonNull(javaLanguage, "javaLanguage");
            this.mode = requireNonNull(mode, "mode");
            this.wantedPresence = Collections.unmodifiableSet(new HashSet<>(wantedPresence));
            Set<JavaDependencyScope> included = collectScopes(wantedPresence);
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

        @Override
        public JavaLanguage getLanguage() {
            return javaLanguage;
        }

        @Override
        public Mode getMode() {
            return mode;
        }

        @Override
        public Set<BuildScope> getWantedPresence() {
            return wantedPresence;
        }

        @Override
        public DependencySelector getDependencySelector() {
            return javaLanguage.getDependencySelector(this);
        }

        @Override
        public DependencyGraphTransformer getDependencyGraphTransformer() {
            return javaLanguage.getDependencyGraphTransformer(this);
        }

        @Override
        public CollectResult postProcess(CollectResult collectResult) {
            return javaLanguage.postProcess(this, collectResult);
        }

        public Set<JavaDependencyScope> getDirectlyIncluded() {
            return directlyIncluded;
        }

        public Set<JavaDependencyScope> getTransitivelyExcluded() {
            return transitivelyExcluded;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            JavaResolutionScope that = (JavaResolutionScope) o;
            return Objects.equals(id, that.id) && Objects.equals(javaLanguage, that.javaLanguage);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, javaLanguage);
        }

        @Override
        public String toString() {
            return getLanguage().getId() + ":" + id;
        }
    }

    public static void main(String... args) {
        JavaLanguage javaLanguage = new JavaLanguage(MavenLevel.Maven3);
        System.out.println();
        javaLanguage.dumpBuildScopes();
        System.out.println();
        javaLanguage.dumpDependencyScopes();
        System.out.println();
        javaLanguage.dumpDependencyScopeDerives();
        System.out.println();
        javaLanguage.dumpResolutionScopes();
    }

    private void dumpBuildScopes() {
        System.out.println(getDescription() + " defined build scopes:");
        buildScopeMatrix.all().stream().forEach(s -> {
            System.out.println(s.getId());
        });
    }

    private void dumpDependencyScopes() {
        System.out.println(getDescription() + " defined dependency scopes:");
        dependencyScopes.values().stream()
                .sorted(Comparator.comparing(DependencyScope::width).reversed())
                .forEach(s -> {
                    System.out.println(s + " (width=" + s.width() + ")");
                    System.out.println("  Presence: "
                            + s.getPresence().stream().map(BuildScope::getId).collect(Collectors.toSet()));
                    System.out.println("  Main project scope: "
                            + s.getMainProjectBuildScope().map(BuildScope::getId).orElse("null"));
                });
    }

    private void dumpDependencyScopeDerives() {
        System.out.println(getDescription() + " defined dependency derive matrix:");
        dependencyScopes.values().stream()
                .sorted(Comparator.comparing(DependencyScope::width).reversed())
                .forEach(s1 -> {
                    dependencyScopes.values().stream()
                            .sorted(Comparator.comparing(DependencyScope::width).reversed())
                            .forEach(s2 -> {
                                System.out.println(
                                        s1.getId() + " w/ parent " + s2.getId() + " -> " + s1.deriveFromParent(s2));
                            });
                });
    }

    private void dumpResolutionScopes() {
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
