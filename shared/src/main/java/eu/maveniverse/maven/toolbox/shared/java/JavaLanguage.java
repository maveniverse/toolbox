/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.java;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.toolbox.shared.DependencyScope;
import eu.maveniverse.maven.toolbox.shared.Language;
import eu.maveniverse.maven.toolbox.shared.ResolutionScope;
import java.util.*;

public final class JavaLanguage implements Language {
    public static final String NAME = "java";

    public static final JavaLanguage MAVEN3 = new JavaLanguage(true);
    public static final JavaLanguage MAVEN4 = new JavaLanguage(false);

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
    private static final String RS_MAIN_RUNTIME_M3 = "main-runtimeM3";
    private static final String RS_MAIN_RUNTIME_M3_PLUS_SYSTEM = "main-runtimeM3PlusSystem";
    private static final String RS_TEST_COMPILE = "test-compile";
    private static final String RS_TEST_RUNTIME = "test-runtime";
    private final boolean systemIsTransitive;
    private final Map<String, JavaDependencyScope> dependencyScopes;
    private final Map<String, JavaResolutionScope> resolutionScopes;

    private JavaLanguage(boolean systemIsTransitive) {
        this.systemIsTransitive = systemIsTransitive;
        this.dependencyScopes = Collections.unmodifiableMap(buildDependencyScopes());
        this.resolutionScopes = Collections.unmodifiableMap(buildResolutionScopes());
    }

    private Map<String, JavaDependencyScope> buildDependencyScopes() {
        HashMap<String, JavaDependencyScope> result = new HashMap<>();
        result.put(DS_NONE, new JavaDependencyScope(DS_NONE, this, false));
        result.put(DS_COMPILE, new JavaDependencyScope(DS_COMPILE, this, true));
        result.put(DS_COMPILE_ONLY, new JavaDependencyScope(DS_COMPILE_ONLY, this, false));
        result.put(DS_RUNTIME, new JavaDependencyScope(DS_RUNTIME, this, true));
        result.put(DS_PROVIDED, new JavaDependencyScope(DS_PROVIDED, this, false));
        result.put(DS_SYSTEM, new JavaDependencyScope(DS_SYSTEM, this, systemIsTransitive));
        result.put(DS_TEST, new JavaDependencyScope(DS_TEST, this, false));
        result.put(DS_TEST_RUNTIME, new JavaDependencyScope(DS_TEST_RUNTIME, this, false));
        result.put(DS_TEST_ONLY, new JavaDependencyScope(DS_TEST_ONLY, this, false));
        return result;
    }

    private Map<String, JavaResolutionScope> buildResolutionScopes() {
        HashMap<String, JavaResolutionScope> result = new HashMap<>();
        result.put(
                RS_NONE,
                new JavaResolutionScope(
                        RS_NONE, this, ResolutionScope.Mode.REMOVE, Collections.emptySet(), dependencyScopes.values()));
        result.put(
                RS_MAIN_COMPILE,
                new JavaResolutionScope(
                        RS_MAIN_COMPILE,
                        this,
                        ResolutionScope.Mode.ELIMINATE,
                        Arrays.asList(
                                dependencyScopes.get(DS_COMPILE),
                                dependencyScopes.get(DS_COMPILE_ONLY),
                                dependencyScopes.get(DS_PROVIDED)),
                        Arrays.asList(dependencyScopes.get(DS_PROVIDED), dependencyScopes.get(DS_TEST))));
        result.put(
                RS_MAIN_COMPILE_PLUS_RUNTIME,
                new JavaResolutionScope(
                        RS_MAIN_COMPILE_PLUS_RUNTIME,
                        this,
                        ResolutionScope.Mode.ELIMINATE,
                        Arrays.asList(
                                dependencyScopes.get(DS_COMPILE),
                                dependencyScopes.get(DS_COMPILE_ONLY),
                                dependencyScopes.get(DS_PROVIDED),
                                dependencyScopes.get(DS_RUNTIME)),
                        Arrays.asList(dependencyScopes.get(DS_PROVIDED), dependencyScopes.get(DS_TEST))));
        result.put(
                RS_MAIN_RUNTIME,
                new JavaResolutionScope(
                        RS_MAIN_RUNTIME,
                        this,
                        ResolutionScope.Mode.REMOVE,
                        Arrays.asList(dependencyScopes.get(DS_COMPILE), dependencyScopes.get(DS_RUNTIME)),
                        Arrays.asList(dependencyScopes.get(DS_PROVIDED), dependencyScopes.get(DS_TEST))));
        result.put(
                RS_MAIN_RUNTIME_PLUS_SYSTEM,
                new JavaResolutionScope(
                        RS_MAIN_RUNTIME_PLUS_SYSTEM,
                        this,
                        ResolutionScope.Mode.REMOVE,
                        Arrays.asList(
                                dependencyScopes.get(DS_COMPILE),
                                dependencyScopes.get(DS_RUNTIME),
                                dependencyScopes.get(DS_SYSTEM)),
                        Arrays.asList(dependencyScopes.get(DS_PROVIDED), dependencyScopes.get(DS_TEST))));
        result.put(
                RS_MAIN_RUNTIME_M3,
                new JavaResolutionScope(
                        RS_MAIN_RUNTIME_M3,
                        this,
                        ResolutionScope.Mode.ELIMINATE,
                        Arrays.asList(
                                dependencyScopes.get(DS_COMPILE),
                                dependencyScopes.get(DS_RUNTIME),
                                dependencyScopes.get(DS_SYSTEM)),
                        Arrays.asList(dependencyScopes.get(DS_PROVIDED), dependencyScopes.get(DS_TEST))));
        result.put(
                RS_MAIN_RUNTIME_M3_PLUS_SYSTEM,
                new JavaResolutionScope(
                        RS_MAIN_RUNTIME_M3_PLUS_SYSTEM,
                        this,
                        ResolutionScope.Mode.ELIMINATE,
                        Arrays.asList(
                                dependencyScopes.get(DS_COMPILE),
                                dependencyScopes.get(DS_COMPILE_ONLY),
                                dependencyScopes.get(DS_RUNTIME),
                                dependencyScopes.get(DS_PROVIDED)),
                        Arrays.asList(dependencyScopes.get(DS_PROVIDED), dependencyScopes.get(DS_TEST))));
        result.put(
                RS_TEST_COMPILE,
                new JavaResolutionScope(
                        RS_TEST_COMPILE,
                        this,
                        ResolutionScope.Mode.ELIMINATE,
                        Arrays.asList(
                                dependencyScopes.get(DS_COMPILE),
                                dependencyScopes.get(DS_PROVIDED),
                                dependencyScopes.get(DS_TEST),
                                dependencyScopes.get(DS_TEST_ONLY)),
                        Arrays.asList(dependencyScopes.get(DS_PROVIDED), dependencyScopes.get(DS_TEST))));
        result.put(
                RS_TEST_RUNTIME,
                new JavaResolutionScope(
                        RS_TEST_RUNTIME,
                        this,
                        ResolutionScope.Mode.ELIMINATE,
                        Arrays.asList(
                                dependencyScopes.get(DS_COMPILE),
                                dependencyScopes.get(DS_PROVIDED),
                                dependencyScopes.get(DS_TEST),
                                dependencyScopes.get(DS_TEST_RUNTIME)),
                        Arrays.asList(dependencyScopes.get(DS_PROVIDED), dependencyScopes.get(DS_TEST))));
        return result;
    }

    @Override
    public String getId() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return systemIsTransitive ? "Java as in Maven3" : "Java as in Maven4";
    }

    @Override
    public Optional<DependencyScope> getDependencyScope(String id) {
        return Optional.ofNullable(dependencyScopes.get(id));
    }

    @Override
    public Collection<? extends DependencyScope> getDependencyScopeUniverse() {
        return dependencyScopes.values();
    }

    @Override
    public Optional<ResolutionScope> getResolutionScope(String id) {
        return Optional.ofNullable(resolutionScopes.get(id));
    }

    @Override
    public Collection<? extends ResolutionScope> getResolutionScopeUniverse() {
        return resolutionScopes.values();
    }

    private static final class JavaDependencyScope implements DependencyScope {
        private final String id;
        private final JavaLanguage javaLanguage;
        private final boolean transitive;

        public JavaDependencyScope(String id, JavaLanguage javaLanguage, boolean transitive) {
            this.id = requireNonNull(id, "id");
            this.javaLanguage = requireNonNull(javaLanguage, "javaLanguage");
            this.transitive = transitive;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public Language getLanguage() {
            return javaLanguage;
        }

        @Override
        public boolean isTransitive() {
            return transitive;
        }

        @Override
        public String toString() {
            return id;
        }
    }

    private static final class JavaResolutionScope implements ResolutionScope {
        private final String id;
        private final JavaLanguage javaLanguage;
        private final Mode mode;
        private final Set<JavaDependencyScope> directlyIncluded;
        private final Set<JavaDependencyScope> transitivelyExcluded;

        private JavaResolutionScope(
                String id,
                JavaLanguage javaLanguage,
                Mode mode,
                Collection<JavaDependencyScope> directlyIncluded,
                Collection<JavaDependencyScope> transitivelyExcluded) {
            this.id = requireNonNull(id, "id");
            this.javaLanguage = requireNonNull(javaLanguage, "javaLanguage");
            this.mode = requireNonNull(mode, "mode");
            this.directlyIncluded = Collections.unmodifiableSet(new HashSet<>(directlyIncluded));
            this.transitivelyExcluded = Collections.unmodifiableSet(new HashSet<>(transitivelyExcluded));
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public Language getLanguage() {
            return javaLanguage;
        }

        @Override
        public Mode getMode() {
            return mode;
        }

        @Override
        public String toString() {
            return id;
        }

        @Override
        public Set<JavaDependencyScope> getDirectlyIncluded() {
            return directlyIncluded;
        }

        @Override
        public Set<JavaDependencyScope> getTransitivelyExcluded() {
            return transitivelyExcluded;
        }
    }
}
