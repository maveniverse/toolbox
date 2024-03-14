package eu.maveniverse.maven.toolbox.shared.internal;

import static eu.maveniverse.maven.toolbox.shared.internal.BuildScopeQuery.all;
import static eu.maveniverse.maven.toolbox.shared.internal.BuildScopeQuery.byBuildPath;
import static eu.maveniverse.maven.toolbox.shared.internal.BuildScopeQuery.byProjectPath;
import static eu.maveniverse.maven.toolbox.shared.internal.BuildScopeQuery.select;
import static eu.maveniverse.maven.toolbox.shared.internal.BuildScopeQuery.singleton;
import static eu.maveniverse.maven.toolbox.shared.internal.BuildScopeQuery.union;
import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.toolbox.shared.DependencyScope;
import eu.maveniverse.maven.toolbox.shared.ResolutionScope;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

public final class MavenConfiguration implements ScopeManagerConfiguration {
    public static final MavenConfiguration MAVEN3 = new MavenConfiguration(MavenLevel.Maven3);
    public static final MavenConfiguration MAVEN4 = new MavenConfiguration(MavenLevel.Maven4);

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

    private enum MavenLevel {
        Maven3,
        Maven4
    }

    private final MavenLevel mavenLevel;

    private MavenConfiguration(MavenLevel mavenLevel) {
        this.mavenLevel = requireNonNull(mavenLevel, "mavenLevel");
    }

    @Override
    public String getId() {
        return mavenLevel.name();
    }

    @Override
    public boolean isStrictDependencyScopes() {
        return false;
    }

    @Override
    public boolean isStrictResolutionScopes() {
        return false;
    }

    @Override
    public boolean isSystemScopeTransitive() {
        return false;
    }

    @Override
    public boolean isBrokenRuntimeResolution() {
        return mavenLevel == MavenLevel.Maven3;
    }

    @Override
    public Optional<String> getSystemDependencyScopeLabel() {
        return Optional.of(DS_SYSTEM);
    }

    @Override
    public BuildScopeSource getBuildScopeSource() {
        return mavenLevel == MavenLevel.Maven3
                ? new BuildScopeMatrixSource(
                        Collections.singletonList(CommonBuilds.PROJECT_PATH_MAIN),
                        Arrays.asList(CommonBuilds.BUILD_PATH_COMPILE, CommonBuilds.BUILD_PATH_RUNTIME),
                        CommonBuilds.MAVEN_TEST_BUILD_SCOPE)
                : new BuildScopeMatrixSource(
                        Arrays.asList(CommonBuilds.PROJECT_PATH_MAIN, CommonBuilds.PROJECT_PATH_TEST),
                        Arrays.asList(CommonBuilds.BUILD_PATH_COMPILE, CommonBuilds.BUILD_PATH_RUNTIME));
    }

    @Override
    public Collection<DependencyScope> buildDependencyScopes(InternalScopeManager internalScopeManager) {
        ArrayList<DependencyScope> result = new ArrayList<>();
        result.add(internalScopeManager.createDependencyScope(DS_COMPILE, true, all()));
        result.add(internalScopeManager.createDependencyScope(
                DS_RUNTIME, true, byBuildPath(CommonBuilds.BUILD_PATH_RUNTIME)));
        result.add(internalScopeManager.createDependencyScope(
                DS_PROVIDED,
                false,
                union(
                        byBuildPath(CommonBuilds.BUILD_PATH_COMPILE),
                        select(CommonBuilds.PROJECT_PATH_TEST, CommonBuilds.BUILD_PATH_RUNTIME))));
        result.add(internalScopeManager.createDependencyScope(
                DS_TEST, false, byProjectPath(CommonBuilds.PROJECT_PATH_TEST)));
        result.add(internalScopeManager.createDependencyScope(DS_SYSTEM, isSystemScopeTransitive(), all()));
        if (mavenLevel == MavenLevel.Maven4) {
            result.add(internalScopeManager.createDependencyScope(DS_NONE, false, Collections.emptySet()));
            result.add(internalScopeManager.createDependencyScope(
                    DS_COMPILE_ONLY,
                    false,
                    singleton(CommonBuilds.PROJECT_PATH_MAIN, CommonBuilds.BUILD_PATH_COMPILE)));
            result.add(internalScopeManager.createDependencyScope(
                    DS_TEST_RUNTIME,
                    false,
                    singleton(CommonBuilds.PROJECT_PATH_TEST, CommonBuilds.BUILD_PATH_RUNTIME)));
            result.add(internalScopeManager.createDependencyScope(
                    DS_TEST_ONLY, false, singleton(CommonBuilds.PROJECT_PATH_TEST, CommonBuilds.BUILD_PATH_COMPILE)));
        }
        return result;
    }

    @Override
    public Collection<ResolutionScope> buildResolutionScopes(InternalScopeManager internalScopeManager) {
        Collection<DependencyScope> allDependencyScopes = internalScopeManager.getDependencyScopeUniverse();
        Collection<DependencyScope> nonTransitiveDependencyScopes =
                allDependencyScopes.stream().filter(s -> !s.isTransitive()).collect(Collectors.toSet());
        DependencyScope system =
                internalScopeManager.getDependencyScope(DS_SYSTEM).orElse(null);

        ArrayList<ResolutionScope> result = new ArrayList<>();
        result.add(internalScopeManager.createResolutionScope(
                RS_NONE,
                InternalScopeManager.Mode.REMOVE,
                Collections.emptySet(),
                Collections.emptySet(),
                allDependencyScopes));
        result.add(internalScopeManager.createResolutionScope(
                RS_MAIN_COMPILE,
                InternalScopeManager.Mode.ELIMINATE,
                singleton(CommonBuilds.PROJECT_PATH_MAIN, CommonBuilds.BUILD_PATH_COMPILE),
                Collections.singletonList(system),
                nonTransitiveDependencyScopes));
        result.add(internalScopeManager.createResolutionScope(
                RS_MAIN_COMPILE_PLUS_RUNTIME,
                InternalScopeManager.Mode.ELIMINATE,
                byProjectPath(CommonBuilds.PROJECT_PATH_MAIN),
                Collections.singletonList(system),
                nonTransitiveDependencyScopes));
        result.add(internalScopeManager.createResolutionScope(
                RS_MAIN_RUNTIME,
                isBrokenRuntimeResolution() ? InternalScopeManager.Mode.ELIMINATE : InternalScopeManager.Mode.REMOVE,
                singleton(CommonBuilds.PROJECT_PATH_MAIN, CommonBuilds.BUILD_PATH_RUNTIME),
                Collections.emptySet(),
                nonTransitiveDependencyScopes));
        result.add(internalScopeManager.createResolutionScope(
                RS_MAIN_RUNTIME_PLUS_SYSTEM,
                isBrokenRuntimeResolution() ? InternalScopeManager.Mode.ELIMINATE : InternalScopeManager.Mode.REMOVE,
                singleton(CommonBuilds.PROJECT_PATH_MAIN, CommonBuilds.BUILD_PATH_RUNTIME),
                Collections.singletonList(system),
                nonTransitiveDependencyScopes));
        result.add(internalScopeManager.createResolutionScope(
                RS_TEST_COMPILE,
                InternalScopeManager.Mode.ELIMINATE,
                select(CommonBuilds.PROJECT_PATH_TEST, CommonBuilds.BUILD_PATH_COMPILE),
                Collections.singletonList(system),
                nonTransitiveDependencyScopes));
        result.add(internalScopeManager.createResolutionScope(
                RS_TEST_RUNTIME,
                InternalScopeManager.Mode.ELIMINATE,
                select(CommonBuilds.PROJECT_PATH_TEST, CommonBuilds.BUILD_PATH_RUNTIME),
                Collections.singletonList(system),
                nonTransitiveDependencyScopes));
        return result;
    }
}
