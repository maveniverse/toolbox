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
    NONE(Collections.emptySet()),
    COMPILE(Arrays.asList(JavaScopes.PROVIDED, JavaScopes.COMPILE, JavaScopes.SYSTEM)),
    COMPILE_PLUS_RUNTIME(Arrays.asList(JavaScopes.PROVIDED, JavaScopes.COMPILE, JavaScopes.SYSTEM, JavaScopes.RUNTIME)),
    RUNTIME(Arrays.asList(JavaScopes.COMPILE, JavaScopes.RUNTIME)),
    RUNTIME_PLUS_SYSTEM(Arrays.asList(JavaScopes.COMPILE, JavaScopes.RUNTIME, JavaScopes.SYSTEM)),
    TEST(Arrays.asList(
            JavaScopes.COMPILE, JavaScopes.SYSTEM, JavaScopes.RUNTIME, JavaScopes.PROVIDED, JavaScopes.TEST));

    private final Set<String> directInclude;

    ResolutionScope(Collection<String> directInclude) {
        this.directInclude = new HashSet<>(directInclude);
    }

    public Predicate<String> getDirectExclude() {
        HashSet<String> directExclude = new HashSet<>(Arrays.asList(
                JavaScopes.COMPILE, JavaScopes.SYSTEM, JavaScopes.RUNTIME, JavaScopes.PROVIDED, JavaScopes.TEST));
        directExclude.removeAll(directInclude);
        return directExclude::contains;
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
