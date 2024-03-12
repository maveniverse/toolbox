package eu.maveniverse.maven.toolbox.shared;

import static java.util.Objects.requireNonNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Generic matrix generator for {@link ProjectPath} and {@link BuildPath} combinations (all of them).
 */
public final class BuildScopeMatrix {
    private final Set<ProjectPath> projectPaths;
    private final Set<BuildPath> buildPaths;
    private final Map<String, BuildScope> buildScopes;

    public BuildScopeMatrix(Collection<ProjectPath> projectPaths, Collection<BuildPath> buildPaths) {
        requireNonNull(projectPaths, "projectPath");
        requireNonNull(buildPaths, "buildPaths");
        if (projectPaths.isEmpty() || buildPaths.isEmpty()) {
            throw new IllegalArgumentException("empty matrix");
        }
        this.projectPaths = Collections.unmodifiableSet(new HashSet<>(projectPaths));
        this.buildPaths = Collections.unmodifiableSet(new HashSet<>(buildPaths));
        HashMap<String, BuildScope> buildScopes = new HashMap<>();
        for (ProjectPath projectPath : projectPaths) {
            for (BuildPath buildPath : buildPaths) {
                String id = createId(projectPath, buildPath);
                buildScopes.put(id, new BuildScope() {
                    @Override
                    public String getId() {
                        return id;
                    }

                    @Override
                    public ProjectPath getProjectPath() {
                        return projectPath;
                    }

                    @Override
                    public BuildPath getBuildPath() {
                        return buildPath;
                    }
                });
            }
        }
        this.buildScopes = Collections.unmodifiableMap(buildScopes);
    }

    private String createId(ProjectPath projectPath, BuildPath buildPath) {
        return projectPath.getId() + "-" + buildPath.getId();
    }

    public Collection<BuildScope> all() {
        return buildScopes.values();
    }

    public Collection<ProjectPath> allProjectPaths() {
        return projectPaths;
    }

    public Collection<BuildPath> allBuildPaths() {
        return buildPaths;
    }

    public Collection<BuildScope> byProjectPath(ProjectPath projectPath) {
        return all().stream()
                .filter(s -> s.getProjectPath().equals(projectPath))
                .collect(Collectors.toSet());
    }

    public Collection<BuildScope> byBuildPath(BuildPath buildPath) {
        return all().stream().filter(s -> s.getBuildPath().equals(buildPath)).collect(Collectors.toSet());
    }

    public Collection<BuildScope> singleton(ProjectPath projectPath, BuildPath buildPath) {
        BuildScope result = buildScopes.get(createId(projectPath, buildPath));
        if (result == null) {
            throw new IllegalArgumentException("no such build scope");
        }
        return Collections.singleton(result);
    }

    public Collection<BuildScope> singletonById(String id) {
        BuildScope result = buildScopes.get(id);
        if (result == null) {
            throw new IllegalArgumentException("no such build scope");
        }
        return Collections.singleton(result);
    }

    public Collection<BuildScope> union(Collection<BuildScope> bs1, Collection<BuildScope> bs2) {
        HashSet<BuildScope> result = new HashSet<>();
        result.addAll(bs1);
        result.addAll(bs2);
        return result;
    }
}
