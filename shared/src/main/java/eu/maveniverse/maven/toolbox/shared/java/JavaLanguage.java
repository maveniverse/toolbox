/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.java;

import eu.maveniverse.maven.toolbox.shared.Atoms;
import java.util.*;

public final class JavaLanguage implements Atoms.Language {
    public static final String NAME = "java";
    public static final JavaLanguage INSTANCE = new JavaLanguage();

    private JavaLanguage() {}

    @Override
    public String getId() {
        return NAME;
    }

    @Override
    public Set<? extends Atoms.LanguageDependencyScope> getLanguageDependencyScopeUniverse() {
        return JavaDependencyScope.UNIVERSE;
    }

    public Set<? extends Atoms.LanguageResolutionScope> getLanguageResolutionScopeUniverse() {
        return JavaResolutionScope.UNIVERSE;
    }

    public static final class JavaDependencyScope extends Atoms.Atom implements Atoms.LanguageDependencyScope {
        private final boolean transitive;
        private final Atoms.DependencyScope dependencyScope;
        private final Set<Atoms.ProjectScope> projectScopes;

        private JavaDependencyScope(
                String id,
                boolean transitive,
                Atoms.DependencyScope dependencyScope,
                Collection<Atoms.ProjectScope> projectScopes) {
            super(id);
            this.transitive = transitive;
            this.dependencyScope = dependencyScope;
            this.projectScopes = Collections.unmodifiableSet(new HashSet<>(projectScopes));
        }

        @Override
        public Atoms.Language getLanguage() {
            return JavaLanguage.INSTANCE;
        }

        @Override
        public boolean isTransitive() {
            return transitive;
        }

        public Atoms.DependencyScope getDependencyScope() {
            return dependencyScope;
        }

        public Set<Atoms.ProjectScope> getProjectScopes() {
            return projectScopes;
        }

        public static final JavaDependencyScope NONE = new JavaDependencyScope(
                "none", false, Atoms.DependencyScope.NONE, Collections.singleton(Atoms.ProjectScope.NONE));
        public static final JavaDependencyScope COMPILE =
                new JavaDependencyScope("compile", true, Atoms.DependencyScope.BOTH, Atoms.ProjectScope.ALL);
        public static final JavaDependencyScope COMPILE_ONLY = new JavaDependencyScope(
                "compileOnly",
                false,
                Atoms.DependencyScope.ONLY_COMPILE,
                Collections.singleton(Atoms.ProjectScope.MAIN));
        public static final JavaDependencyScope RUNTIME =
                new JavaDependencyScope("runtime", true, Atoms.DependencyScope.ONLY_RUNTIME, Atoms.ProjectScope.ALL);
        public static final JavaDependencyScope PROVIDED =
                new JavaDependencyScope("provided", false, Atoms.DependencyScope.ONLY_COMPILE, Atoms.ProjectScope.ALL);
        public static final JavaDependencyScope SYSTEM =
                new JavaDependencyScope("system", false, Atoms.DependencyScope.BOTH, Atoms.ProjectScope.ALL);
        public static final JavaDependencyScope TEST = new JavaDependencyScope(
                "test", false, Atoms.DependencyScope.BOTH, Collections.singleton(Atoms.ProjectScope.TEST));
        public static final JavaDependencyScope TEST_RUNTIME = new JavaDependencyScope(
                "testRuntime",
                false,
                Atoms.DependencyScope.ONLY_RUNTIME,
                Collections.singleton(Atoms.ProjectScope.TEST));
        public static final JavaDependencyScope TEST_ONLY = new JavaDependencyScope(
                "testOnly", false, Atoms.DependencyScope.ONLY_COMPILE, Collections.singleton(Atoms.ProjectScope.TEST));

        public static final Set<JavaDependencyScope> UNIVERSE = Collections.unmodifiableSet(new HashSet<>(
                Arrays.asList(NONE, COMPILE, COMPILE_ONLY, RUNTIME, PROVIDED, SYSTEM, TEST, TEST_RUNTIME, TEST_ONLY)));
    }

    public static final class JavaResolutionScope extends Atoms.Atom implements Atoms.LanguageResolutionScope {
        public static final JavaResolutionScope MAIN_COMPILE = new JavaResolutionScope(
                "main-compile", Atoms.ProjectScope.MAIN, Atoms.ResolutionScope.COMPILE, Atoms.ResolutionMode.ELIMINATE);
        public static final JavaResolutionScope MAIN_RUNTIME = new JavaResolutionScope(
                "main-runtime", Atoms.ProjectScope.MAIN, Atoms.ResolutionScope.RUNTIME, Atoms.ResolutionMode.REMOVE);
        public static final JavaResolutionScope MAIN_RUNTIME_M3 = new JavaResolutionScope(
                "main-runtime-m3",
                Atoms.ProjectScope.MAIN,
                Atoms.ResolutionScope.RUNTIME,
                Atoms.ResolutionMode.ELIMINATE);
        public static final JavaResolutionScope TEST_COMPILE = new JavaResolutionScope(
                "test-compile", Atoms.ProjectScope.TEST, Atoms.ResolutionScope.COMPILE, Atoms.ResolutionMode.ELIMINATE);
        public static final JavaResolutionScope TEST_RUNTIME = new JavaResolutionScope(
                "test-runtime", Atoms.ProjectScope.TEST, Atoms.ResolutionScope.RUNTIME, Atoms.ResolutionMode.REMOVE);

        public static final Set<JavaResolutionScope> UNIVERSE = Collections.unmodifiableSet(
                new HashSet<>(Arrays.asList(MAIN_COMPILE, MAIN_RUNTIME, MAIN_RUNTIME_M3, TEST_COMPILE, TEST_RUNTIME)));

        private final Atoms.ProjectScope projectScope;
        private final Atoms.ResolutionScope resolutionScope;
        private final Atoms.ResolutionMode resolutionMode;

        public JavaResolutionScope(
                String id,
                Atoms.ProjectScope projectScope,
                Atoms.ResolutionScope resolutionScope,
                Atoms.ResolutionMode resolutionMode) {
            super(id);
            this.projectScope = projectScope;
            this.resolutionScope = resolutionScope;
            this.resolutionMode = resolutionMode;
        }

        @Override
        public Atoms.Language getLanguage() {
            return JavaLanguage.INSTANCE;
        }

        @Override
        public Atoms.ProjectScope getProjectScope() {
            return projectScope;
        }

        @Override
        public Atoms.ResolutionScope getResolutionScope() {
            return resolutionScope;
        }

        @Override
        public Atoms.ResolutionMode getResolutionMode() {
            return resolutionMode;
        }
    }
}
