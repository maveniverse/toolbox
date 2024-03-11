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
        return JavaDependencyScope.ALL;
    }

    public Set<? extends Atoms.LanguageResolutionScope> getLanguageResolutionScopeUniverse() {
        return JavaResolutionScope.ALL;
    }

    public static final class JavaDependencyScope extends Atoms.Atom implements Atoms.LanguageDependencyScope {
        private final boolean transitive;
        private final Set<Atoms.DependencyScope> dependencyScopes;
        private final Set<Atoms.ProjectScope> projectScopes;

        private JavaDependencyScope(
                String id,
                boolean transitive,
                Collection<Atoms.DependencyScope> dependencyScopes,
                Collection<Atoms.ProjectScope> projectScopes) {
            super(id);
            this.transitive = transitive;
            this.dependencyScopes = Collections.unmodifiableSet(new HashSet<>(dependencyScopes));
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

        public Set<Atoms.DependencyScope> getDependencyScopes() {
            return dependencyScopes;
        }

        public Set<Atoms.ProjectScope> getProjectScopes() {
            return projectScopes;
        }

        public static final JavaDependencyScope NONE = new JavaDependencyScope(
                "none", false, Collections.singleton(Atoms.DependencyScope.NONE), Collections.emptySet());
        public static final JavaDependencyScope COMPILE = new JavaDependencyScope(
                "compile", true, Collections.singleton(Atoms.DependencyScope.BOTH), Atoms.ProjectScope.ALL);
        public static final JavaDependencyScope COMPILE_ONLY = new JavaDependencyScope(
                "compileOnly",
                false,
                Collections.singleton(Atoms.DependencyScope.ONLY_COMPILE),
                Collections.singleton(Atoms.ProjectScope.MAIN));
        public static final JavaDependencyScope RUNTIME = new JavaDependencyScope(
                "runtime", true, Collections.singleton(Atoms.DependencyScope.ONLY_RUNTIME), Atoms.ProjectScope.ALL);
        public static final JavaDependencyScope PROVIDED = new JavaDependencyScope(
                "provided", false, Collections.singleton(Atoms.DependencyScope.ONLY_COMPILE), Atoms.ProjectScope.ALL);
        public static final JavaDependencyScope SYSTEM = new JavaDependencyScope(
                "system", false, Collections.singleton(Atoms.DependencyScope.BOTH), Atoms.ProjectScope.ALL);
        public static final JavaDependencyScope TEST = new JavaDependencyScope(
                "test",
                false,
                Collections.singleton(Atoms.DependencyScope.BOTH),
                Collections.singleton(Atoms.ProjectScope.TEST));
        public static final JavaDependencyScope TEST_RUNTIME = new JavaDependencyScope(
                "testRuntime",
                false,
                Collections.singleton(Atoms.DependencyScope.ONLY_RUNTIME),
                Collections.singleton(Atoms.ProjectScope.TEST));
        public static final JavaDependencyScope TEST_ONLY = new JavaDependencyScope(
                "testOnly",
                false,
                Collections.singleton(Atoms.DependencyScope.ONLY_COMPILE),
                Collections.singleton(Atoms.ProjectScope.TEST));

        public static final Set<JavaDependencyScope> ALL = Collections.unmodifiableSet(new HashSet<>(
                Arrays.asList(NONE, COMPILE, COMPILE_ONLY, RUNTIME, PROVIDED, SYSTEM, TEST, TEST_RUNTIME, TEST_ONLY)));
    }

    private static final class JavaResolutionScope extends Atoms.Atom implements Atoms.LanguageResolutionScope {
        private final Set<Atoms.ProjectScope> projectScopes;
        private final Set<Atoms.ResolutionScope> resolutionScopes;
        private final Atoms.ResolutionMode resolutionMode;

        private JavaResolutionScope(
                String id,
                Collection<Atoms.ProjectScope> projectScopes,
                Collection<Atoms.ResolutionScope> resolutionScopes,
                Atoms.ResolutionMode resolutionMode) {
            super(id);
            this.projectScopes = Collections.unmodifiableSet(new HashSet<>(projectScopes));
            this.resolutionScopes = Collections.unmodifiableSet(new HashSet<>(resolutionScopes));
            this.resolutionMode = resolutionMode;
        }

        @Override
        public Atoms.Language getLanguage() {
            return JavaLanguage.INSTANCE;
        }

        @Override
        public Set<Atoms.ProjectScope> getProjectScopes() {
            return projectScopes;
        }

        @Override
        public Set<Atoms.ResolutionScope> getResolutionScopes() {
            return resolutionScopes;
        }

        @Override
        public Atoms.ResolutionMode getResolutionMode() {
            return resolutionMode;
        }

        public JavaResolutionScope plus(JavaDependencyScope dependencyScope) {
            if (this == NONE) {
                throw new IllegalStateException("NONE resolution scope is immutable");
            } else if (this == EMPTY) {
                return new JavaResolutionScope(
                        dependencyScope.getId(),
                        dependencyScope.getProjectScopes(),
                        dependencyScope.getMemberOf(),
                        resolutionMode);
            }
            if (this.projectScopes.containsAll(dependencyScope.getProjectScopes())
                    && this.resolutionScopes.containsAll(dependencyScope.getMemberOf())) {
                return this;
            }
            HashSet<Atoms.ProjectScope> projectScopes = new HashSet<>(this.projectScopes);
            projectScopes.addAll(dependencyScope.getProjectScopes());
            HashSet<Atoms.ResolutionScope> resolutionScopes = new HashSet<>(this.resolutionScopes);
            resolutionScopes.addAll(dependencyScope.getMemberOf());
            return new JavaResolutionScope(
                    getId() + "+" + dependencyScope.getId(), projectScopes, resolutionScopes, resolutionMode);
        }

        public static final JavaResolutionScope NONE = new JavaResolutionScope(
                "none", Collections.emptySet(), Collections.emptySet(), Atoms.ResolutionMode.ELIMINATE);
        public static final JavaResolutionScope EMPTY = new JavaResolutionScope(
                "empty", Collections.emptySet(), Collections.emptySet(), Atoms.ResolutionMode.ELIMINATE);
        public static final JavaResolutionScope MAIN_COMPILE = new JavaResolutionScope(
                "main-compile",
                Collections.singleton(Atoms.ProjectScope.MAIN),
                Collections.singleton(Atoms.ResolutionScope.COMPILE),
                Atoms.ResolutionMode.ELIMINATE);
        public static final JavaResolutionScope MAIN_COMPILE_PLUS_RUNTIME =
                MAIN_COMPILE.plus(JavaDependencyScope.RUNTIME);
        public static final JavaResolutionScope MAIN_RUNTIME = new JavaResolutionScope(
                "main-runtime",
                Collections.singleton(Atoms.ProjectScope.MAIN),
                Collections.singleton(Atoms.ResolutionScope.RUNTIME),
                Atoms.ResolutionMode.REMOVE);
        public static final JavaResolutionScope MAIN_RUNTIME_PLUS_SYSTEM =
                MAIN_RUNTIME.plus(JavaDependencyScope.SYSTEM);
        public static final JavaResolutionScope MAIN_RUNTIME_M3 = new JavaResolutionScope(
                "main-runtime-m3",
                Collections.singleton(Atoms.ProjectScope.MAIN),
                Collections.singleton(Atoms.ResolutionScope.RUNTIME),
                Atoms.ResolutionMode.ELIMINATE);
        public static final JavaResolutionScope TEST_COMPILE = new JavaResolutionScope(
                "test-compile",
                Collections.singleton(Atoms.ProjectScope.TEST),
                Collections.singleton(Atoms.ResolutionScope.COMPILE),
                Atoms.ResolutionMode.ELIMINATE);
        public static final JavaResolutionScope TEST_RUNTIME = new JavaResolutionScope(
                "test-runtime",
                Collections.singleton(Atoms.ProjectScope.TEST),
                Collections.singleton(Atoms.ResolutionScope.RUNTIME),
                Atoms.ResolutionMode.REMOVE);

        public static final Set<JavaResolutionScope> ALL = Collections.unmodifiableSet(new HashSet<>(
                Arrays.asList(NONE, EMPTY, MAIN_COMPILE, MAIN_RUNTIME, MAIN_RUNTIME_M3, TEST_COMPILE, TEST_RUNTIME)));
    }
}
