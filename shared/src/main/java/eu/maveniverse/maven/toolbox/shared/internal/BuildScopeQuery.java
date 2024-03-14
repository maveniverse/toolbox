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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * Build scope query.
 */
public final class BuildScopeQuery {
    public enum Mode {
        ALL,
        BY_PROJECT_PATH,
        BY_BUILD_PATH,
        SELECT,
        SINGLETON;

        private void validate(ProjectPath projectPath, BuildPath buildPath) {
            if ((this == ALL) && (projectPath != null || buildPath != null)) {
                throw new IllegalArgumentException(this.name() + " requires no parameter");
            } else if (this == BY_PROJECT_PATH && (projectPath == null || buildPath != null)) {
                throw new IllegalArgumentException(this.name() + " requires project path parameter only");
            } else if (this == BY_BUILD_PATH && (projectPath != null || buildPath == null)) {
                throw new IllegalArgumentException(this.name() + " requires build path parameter only");
            } else if ((this == SELECT || this == SINGLETON) && (projectPath == null || buildPath == null)) {
                throw new IllegalArgumentException(this.name() + " requires both parameters");
            }
        }
    }

    private final Mode mode;
    private final ProjectPath projectPath;
    private final BuildPath buildPath;

    private BuildScopeQuery(Mode mode, ProjectPath projectPath, BuildPath buildPath) {
        this.mode = requireNonNull(mode, "mode");
        mode.validate(projectPath, buildPath);
        this.projectPath = projectPath;
        this.buildPath = buildPath;
    }

    public Mode getMode() {
        return mode;
    }

    public ProjectPath getProjectPath() {
        return projectPath;
    }

    public BuildPath getBuildPath() {
        return buildPath;
    }

    @Override
    public String toString() {
        if ((mode == Mode.ALL)) {
            return mode.name();
        } else if (mode == Mode.BY_PROJECT_PATH) {
            return mode.name() + "(" + projectPath.getId() + ")";
        } else if (mode == Mode.BY_BUILD_PATH) {
            return mode.name() + "(" + buildPath.getId() + ")";
        } else {
            return mode.name() + "(" + projectPath.getId() + ", " + buildPath.getId() + ")";
        }
    }

    public static Collection<BuildScopeQuery> all() {
        return Collections.singleton(new BuildScopeQuery(Mode.ALL, null, null));
    }

    public static Collection<BuildScopeQuery> byProjectPath(ProjectPath projectPath) {
        return Collections.singleton(new BuildScopeQuery(Mode.BY_PROJECT_PATH, projectPath, null));
    }

    public static Collection<BuildScopeQuery> byBuildPath(BuildPath buildPath) {
        return Collections.singleton(new BuildScopeQuery(Mode.BY_BUILD_PATH, null, buildPath));
    }

    public static Collection<BuildScopeQuery> select(ProjectPath projectPath, BuildPath buildPath) {
        return Collections.singleton(new BuildScopeQuery(Mode.SELECT, projectPath, buildPath));
    }

    public static Collection<BuildScopeQuery> singleton(ProjectPath projectPath, BuildPath buildPath) {
        return Collections.singleton(new BuildScopeQuery(Mode.SINGLETON, projectPath, buildPath));
    }

    @SafeVarargs
    public static Collection<BuildScopeQuery> union(Collection<BuildScopeQuery>... buildScopeQueries) {
        HashSet<BuildScopeQuery> result = new HashSet<>();
        Arrays.asList(buildScopeQueries).forEach(result::addAll);
        return result;
    }
}
