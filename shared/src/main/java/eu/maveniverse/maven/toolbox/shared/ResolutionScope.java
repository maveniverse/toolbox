/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;

/**
 * Generic resolution scope abstraction.
 * <p>
 * Uses Maven3 mojo resolution scopes as template.
 */
public enum ResolutionScope {
    /**
     * None.
     */
    NONE(
            false,
            Collections.emptySet(),
            Arrays.asList(
                    JavaScopes.COMPILE, JavaScopes.SYSTEM, JavaScopes.RUNTIME, JavaScopes.PROVIDED, JavaScopes.TEST)),
    /**
     * This scope contains all dependencies needed for compilation.
     */
    COMPILE(
            false,
            Arrays.asList(JavaScopes.PROVIDED, JavaScopes.COMPILE, JavaScopes.SYSTEM),
            Arrays.asList(JavaScopes.PROVIDED, JavaScopes.TEST)),
    /**
     * Same as {@link #COMPILE} with addition of {@link JavaScopes#RUNTIME}.
     */
    COMPILE_PLUS_RUNTIME(
            false,
            Arrays.asList(JavaScopes.PROVIDED, JavaScopes.COMPILE, JavaScopes.SYSTEM, JavaScopes.RUNTIME),
            Arrays.asList(JavaScopes.PROVIDED, JavaScopes.TEST)),
    /**
     * This scope contains all dependencies needed at runtime.
     */
    RUNTIME(
            true,
            Arrays.asList(JavaScopes.COMPILE, JavaScopes.RUNTIME),
            Arrays.asList(JavaScopes.PROVIDED, JavaScopes.TEST)),
    /**
     * Same as {@link #RUNTIME} with addition of {@link JavaScopes#SYSTEM}.
     */
    RUNTIME_PLUS_SYSTEM(
            true,
            Arrays.asList(JavaScopes.COMPILE, JavaScopes.RUNTIME, JavaScopes.SYSTEM),
            Arrays.asList(JavaScopes.PROVIDED, JavaScopes.TEST)),
    /**
     * This scope contains all dependencies needed for test compilation and test runtime.
     */
    TEST(
            false,
            Arrays.asList(
                    JavaScopes.COMPILE, JavaScopes.SYSTEM, JavaScopes.RUNTIME, JavaScopes.PROVIDED, JavaScopes.TEST),
            Arrays.asList(JavaScopes.PROVIDED, JavaScopes.TEST));

    private final boolean eliminateTest;
    private final Set<String> directInclude;
    private final Set<String> transitiveExclude;

    ResolutionScope(boolean eliminateTest, Collection<String> directInclude, Collection<String> transitiveExclude) {
        this.eliminateTest = eliminateTest;
        this.directInclude = new HashSet<>(directInclude);
        this.transitiveExclude = new HashSet<>(transitiveExclude);
    }

    public boolean isEliminateTest() {
        return eliminateTest;
    }

    public Set<String> getDirectInclude() {
        return directInclude;
    }

    public Set<String> getTransitiveExclude() {
        return transitiveExclude;
    }

    public DependencyFilter getDependencyFilter() {
        return DependencyFilterUtils.classpathFilter(directInclude);
    }

    public static ResolutionScope parse(String value) throws IllegalArgumentException {
        requireNonNull(value, "value");
        try {
            return ResolutionScope.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "No such scope; available scopes are: " + Arrays.toString(ResolutionScope.values()), e);
        }
    }
}
