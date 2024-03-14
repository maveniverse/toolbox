/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.toolbox.shared.BuildPath;
import eu.maveniverse.maven.toolbox.shared.ProjectPath;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Generic matrix generator for {@link ProjectPath} and {@link BuildPath} combinations (all of them).
 */
public final class BuildScopeMatrixSource implements BuildScopeSource {
    private final Set<ProjectPath> projectPaths;
    private final Set<BuildPath> buildPaths;
    private final Map<String, BuildScope> buildScopes;

    public BuildScopeMatrixSource(
            Collection<ProjectPath> projectPaths, Collection<BuildPath> buildPaths, BuildScope... extras) {
        requireNonNull(projectPaths, "projectPath");
        requireNonNull(buildPaths, "buildPaths");
        if (projectPaths.isEmpty() || buildPaths.isEmpty()) {
            throw new IllegalArgumentException("empty matrix");
        }
        HashMap<String, BuildScope> buildScopes = new HashMap<>();
        AtomicInteger counter = new AtomicInteger(0);
        buildPaths.stream().sorted(Comparator.comparing(BuildPath::order)).forEach(buildPath -> {
            Stream<ProjectPath> projectPathStream;
            if (buildPath.isReverse()) {
                projectPathStream = projectPaths.stream().sorted(Comparator.comparing(ProjectPath::reverseOrder));
            } else {
                projectPathStream = projectPaths.stream().sorted(Comparator.comparing(ProjectPath::order));
            }
            projectPathStream.forEach(projectPath -> {
                String id = createId(projectPath, buildPath);
                buildScopes.put(
                        id,
                        new BuildScopeImpl(
                                id,
                                Collections.singleton(projectPath),
                                Collections.singleton(buildPath),
                                counter.incrementAndGet()));
            });
        });
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

    @Override
    public Collection<BuildScope> query(Collection<BuildScopeQuery> queries) {
        HashSet<BuildScope> result = new HashSet<>();
        for (BuildScopeQuery query : queries) {
            switch (query.getMode()) {
                case ALL:
                    result.addAll(all());
                    break; // we added all, whatever is after this, is unimportant
                case BY_PROJECT_PATH:
                    result.addAll(byProjectPath(query.getProjectPath()));
                    continue;
                case BY_BUILD_PATH:
                    result.addAll(byBuildPath(query.getBuildPath()));
                    continue;
                case SELECT:
                    result.addAll(select(query.getProjectPath(), query.getBuildPath()));
                    continue;
                case SINGLETON:
                    result.addAll(singleton(query.getProjectPath(), query.getBuildPath()));
                    continue;
                default:
                    throw new IllegalArgumentException("Unsupported query");
            }
        }
        return result;
    }

    @Override
    public Collection<ProjectPath> allProjectPaths() {
        return projectPaths;
    }

    @Override
    public Collection<BuildPath> allBuildPaths() {
        return buildPaths;
    }

    private Collection<BuildScope> all() {
        return buildScopes.values();
    }

    private Collection<BuildScope> byProjectPath(ProjectPath projectPath) {
        return all().stream()
                .filter(s -> s.getProjectPaths().contains(projectPath))
                .collect(Collectors.toSet());
    }

    private Collection<BuildScope> byBuildPath(BuildPath buildPath) {
        return all().stream().filter(s -> s.getBuildPaths().contains(buildPath)).collect(Collectors.toSet());
    }

    private Collection<BuildScope> singleton(ProjectPath projectPath, BuildPath buildPath) {
        BuildScope result = buildScopes.get(createId(projectPath, buildPath));
        if (result == null) {
            throw new IllegalArgumentException("no such build scope");
        }
        return Collections.singleton(result);
    }

    private Collection<BuildScope> select(ProjectPath projectPath, BuildPath buildPath) {
        HashSet<BuildScope> result = new HashSet<>();
        buildScopes.values().stream()
                .filter(s -> s.getProjectPaths().contains(projectPath)
                        && s.getBuildPaths().contains(buildPath))
                .forEach(result::add);
        return result;
    }

    private static final class BuildScopeImpl implements BuildScope {
        private final String id;
        private final Set<ProjectPath> projectPaths;
        private final Set<BuildPath> buildPaths;
        private final int order;

        private BuildScopeImpl(String id, Set<ProjectPath> projectPaths, Set<BuildPath> buildPaths, int order) {
            this.id = id;
            this.projectPaths = projectPaths;
            this.buildPaths = buildPaths;
            this.order = order;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public Set<ProjectPath> getProjectPaths() {
            return projectPaths;
        }

        @Override
        public Set<BuildPath> getBuildPaths() {
            return buildPaths;
        }

        @Override
        public int order() {
            return order;
        }
    }
}
