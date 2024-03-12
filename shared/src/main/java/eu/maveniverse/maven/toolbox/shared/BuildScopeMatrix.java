/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
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

    public BuildScopeMatrix(
            Collection<ProjectPath> projectPaths, Collection<BuildPath> buildPaths, BuildScope... extras) {
        requireNonNull(projectPaths, "projectPath");
        requireNonNull(buildPaths, "buildPaths");
        if (projectPaths.isEmpty() || buildPaths.isEmpty()) {
            throw new IllegalArgumentException("empty matrix");
        }
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
                    public Set<ProjectPath> getProjectPaths() {
                        return Collections.singleton(projectPath);
                    }

                    @Override
                    public Set<BuildPath> getBuildPaths() {
                        return Collections.singleton(buildPath);
                    }
                });
            }
        }
        for (BuildScope extra : extras) {
            buildScopes.put(extra.getId(), extra);
        }
        this.buildScopes = Collections.unmodifiableMap(buildScopes);

        // now collect all paths
        HashSet<ProjectPath> pp = new HashSet<>(projectPaths);
        HashSet<BuildPath> bp = new HashSet<>(buildPaths);
        buildScopes.values().forEach(s -> {
            pp.addAll(s.getProjectPaths());
            bp.addAll(s.getBuildPaths());
        });
        this.projectPaths = Collections.unmodifiableSet(pp);
        this.buildPaths = Collections.unmodifiableSet(bp);
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
                .filter(s -> s.getProjectPaths().contains(projectPath))
                .collect(Collectors.toSet());
    }

    public Collection<BuildScope> byBuildPath(BuildPath buildPath) {
        return all().stream().filter(s -> s.getBuildPaths().contains(buildPath)).collect(Collectors.toSet());
    }

    public Collection<BuildScope> singleton(ProjectPath projectPath, BuildPath buildPath) {
        BuildScope result = buildScopes.get(createId(projectPath, buildPath));
        if (result == null) {
            throw new IllegalArgumentException("no such build scope");
        }
        return Collections.singleton(result);
    }

    public Collection<BuildScope> select(ProjectPath projectPath, BuildPath buildPath) {
        HashSet<BuildScope> result = new HashSet<>();
        buildScopes.values().stream()
                .filter(s -> s.getProjectPaths().contains(projectPath)
                        && s.getBuildPaths().contains(buildPath))
                .forEach(result::add);
        return result;
    }

    public Collection<BuildScope> union(Collection<BuildScope> bs1, Collection<BuildScope> bs2) {
        HashSet<BuildScope> result = new HashSet<>();
        result.addAll(bs1);
        result.addAll(bs2);
        return result;
    }
}
