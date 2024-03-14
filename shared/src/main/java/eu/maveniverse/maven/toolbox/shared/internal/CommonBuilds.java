/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import eu.maveniverse.maven.toolbox.shared.BuildPath;
import eu.maveniverse.maven.toolbox.shared.ProjectPath;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Set of constants meant to be used in "common builds".
 */
public final class CommonBuilds {
    private CommonBuilds() {}

    private abstract static class Label {
        private final String id;

        protected Label(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Label label = (Label) o;
            return Objects.equals(id, label.id);
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

    private static final class ProjectPathImpl extends Label implements ProjectPath {
        private final int order;
        private final int reverseOrder;

        private ProjectPathImpl(String id, int order, int reverseOrder) {
            super(id);
            this.order = order;
            this.reverseOrder = reverseOrder;
        }

        @Override
        public int order() {
            return order;
        }

        @Override
        public int reverseOrder() {
            return reverseOrder;
        }
    }

    private static final class BuildPathImpl extends Label implements BuildPath {
        private final boolean reverse;
        private final int order;

        private BuildPathImpl(String id, boolean reverse, int order) {
            super(id);
            this.reverse = reverse;
            this.order = order;
        }

        @Override
        public boolean isReverse() {
            return reverse;
        }

        @Override
        public int order() {
            return order;
        }
    }

    private static final class BuildScopeImpl extends Label implements BuildScope {
        private final Set<ProjectPath> projectPaths;
        private final Set<BuildPath> buildPaths;
        private final int order;

        private BuildScopeImpl(String id, Set<ProjectPath> projectPaths, Set<BuildPath> buildPaths, int order) {
            super(id);
            this.projectPaths = projectPaths;
            this.buildPaths = buildPaths;
            this.order = order;
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

    /**
     * A common "main" project path.
     */
    public static final ProjectPath PROJECT_PATH_MAIN = new ProjectPathImpl("main", 1, 3);

    /**
     * A common "test" project path.
     */
    public static final ProjectPath PROJECT_PATH_TEST = new ProjectPathImpl("test", 2, 1);

    /**
     * A common "it" project path.
     */
    public static final ProjectPath PROJECT_PATH_IT = new ProjectPathImpl("it", 3, 2);

    /**
     * A common "preprocess" build path.
     */
    public static final BuildPath BUILD_PATH_PREPROCESS = new BuildPathImpl("preprocess", false, 1);

    /**
     * A common "compile" build path.
     */
    public static final BuildPath BUILD_PATH_COMPILE = new BuildPathImpl("compile", false, 2);

    /**
     * A common "runtime" build path.
     */
    public static final BuildPath BUILD_PATH_RUNTIME = new BuildPathImpl("runtime", true, 3);

    /**
     * Maven2/Maven3 special build scope: it did not distinguish between "test compile"
     * and "test runtime", but lumped both together into "test".
     */
    public static final BuildScope MAVEN_TEST_BUILD_SCOPE = new BuildScopeImpl(
            "test",
            Collections.singleton(PROJECT_PATH_TEST),
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList(BUILD_PATH_COMPILE, BUILD_PATH_RUNTIME))),
            10);
}
