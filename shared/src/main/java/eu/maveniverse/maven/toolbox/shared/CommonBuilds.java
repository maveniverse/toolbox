/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Set of constants meant to be used in "common builds".
 */
public final class CommonBuilds {
    private CommonBuilds() {}

    /**
     * A common "main" project path.
     */
    public static final ProjectPath PROJECT_PATH_MAIN = new ProjectPath() {
        @Override
        public String getId() {
            return "main";
        }

        @Override
        public int order() {
            return 1;
        }
    };

    /**
     * A common "test" project path.
     */
    public static final ProjectPath PROJECT_PATH_TEST = new ProjectPath() {
        @Override
        public String getId() {
            return "test";
        }

        @Override
        public int order() {
            return 2;
        }
    };

    /**
     * A common "it" project path.
     */
    public static final ProjectPath PROJECT_PATH_IT = new ProjectPath() {
        @Override
        public String getId() {
            return "it";
        }

        @Override
        public int order() {
            return 3;
        }
    };

    /**
     * A common "preprocess" build path.
     */
    public static final BuildPath BUILD_PATH_PREPROCESS = new BuildPath() {
        @Override
        public String getId() {
            return "preprocess";
        }

        @Override
        public boolean isReverse() {
            return false;
        }

        @Override
        public int order() {
            return 1;
        }
    };

    /**
     * A common "compile" build path.
     */
    public static final BuildPath BUILD_PATH_COMPILE = new BuildPath() {
        @Override
        public String getId() {
            return "compile";
        }

        @Override
        public boolean isReverse() {
            return false;
        }

        @Override
        public int order() {
            return 2;
        }
    };

    /**
     * A common "runtime" build path.
     */
    public static final BuildPath BUILD_PATH_RUNTIME = new BuildPath() {
        @Override
        public String getId() {
            return "runtime";
        }

        @Override
        public boolean isReverse() {
            return true;
        }

        @Override
        public int order() {
            return 2;
        }
    };

    /**
     * Maven2/Maven3 special build scope: it did not distinguish between "test compile"
     * and "test runtime", but lumped both together into "test".
     */
    public static final BuildScope MAVEN_TEST_BUILD_SCOPE = new BuildScope() {
        @Override
        public String getId() {
            return "test";
        }

        @Override
        public Set<ProjectPath> getProjectPaths() {
            return Collections.singleton(PROJECT_PATH_TEST);
        }

        @Override
        public Set<BuildPath> getBuildPaths() {
            return new HashSet<>(Arrays.asList(BUILD_PATH_COMPILE, BUILD_PATH_RUNTIME));
        }

        @Override
        public int order() {
            return 10;
        }
    };
}
