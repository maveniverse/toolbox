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
import java.util.Locale;
import java.util.function.Predicate;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;

/**
 * Generic resolution scope abstraction.
 * <p>
 * Uses Maven3 mojo resolution scopes as template.
 */
public enum ResolutionScope {
    NONE(s -> false, DependencyFilterUtils.classpathFilter()),
    COMPILE(
            s -> JavaScopes.PROVIDED.equals(s) || JavaScopes.COMPILE.equals(s) || JavaScopes.SYSTEM.equals(s),
            DependencyFilterUtils.classpathFilter(JavaScopes.PROVIDED, JavaScopes.COMPILE, JavaScopes.SYSTEM)),
    COMPILE_PLUS_RUNTIME(
            s -> JavaScopes.PROVIDED.equals(s)
                    || JavaScopes.COMPILE.equals(s)
                    || JavaScopes.SYSTEM.equals(s)
                    || JavaScopes.RUNTIME.equals(s),
            DependencyFilterUtils.classpathFilter(
                    JavaScopes.PROVIDED, JavaScopes.COMPILE, JavaScopes.SYSTEM, JavaScopes.RUNTIME)),
    RUNTIME(
            s -> JavaScopes.COMPILE.equals(s) || JavaScopes.RUNTIME.equals(s),
            DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE, JavaScopes.RUNTIME)),
    RUNTIME_PLUS_SYSTEM(
            s -> JavaScopes.COMPILE.equals(s) || JavaScopes.RUNTIME.equals(s) || JavaScopes.SYSTEM.equals(s),
            DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE, JavaScopes.RUNTIME, JavaScopes.SYSTEM)),
    TEST(
            s -> true,
            DependencyFilterUtils.classpathFilter(
                    JavaScopes.PROVIDED, JavaScopes.COMPILE, JavaScopes.SYSTEM, JavaScopes.RUNTIME, JavaScopes.TEST));

    private final Predicate<String> directInclude;
    private final DependencyFilter dependencyFilter;

    ResolutionScope(Predicate<String> directInclude, DependencyFilter dependencyFilter) {
        this.directInclude = directInclude;
        this.dependencyFilter = dependencyFilter;
    }

    public Predicate<String> getDirectInclude() {
        return directInclude;
    }

    public DependencyFilter getDependencyFilter() {
        return dependencyFilter;
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
