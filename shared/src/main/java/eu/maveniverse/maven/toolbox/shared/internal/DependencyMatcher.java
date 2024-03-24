/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.Predicate;
import org.eclipse.aether.graph.Dependency;

/**
 * Dependency matcher.
 */
public interface DependencyMatcher extends Predicate<Dependency> {
    static DependencyMatcher not(DependencyMatcher matcher) {
        return new DependencyMatcher() {
            @Override
            public boolean test(Dependency dependency) {
                return !matcher.test(dependency);
            }
        };
    }

    static DependencyMatcher and(DependencyMatcher... matchers) {
        return and(Arrays.asList(matchers));
    }

    static DependencyMatcher and(Collection<DependencyMatcher> matchers) {
        return new DependencyMatcher() {
            @Override
            public boolean test(Dependency dependency) {
                for (DependencyMatcher matcher : matchers) {
                    if (!matcher.test(dependency)) {
                        return false;
                    }
                }
                return true;
            }
        };
    }

    static DependencyMatcher or(DependencyMatcher... matchers) {
        return or(Arrays.asList(matchers));
    }

    static DependencyMatcher or(Collection<DependencyMatcher> matchers) {
        return new DependencyMatcher() {
            @Override
            public boolean test(Dependency dependency) {
                for (DependencyMatcher matcher : matchers) {
                    if (matcher.test(dependency)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    static DependencyMatcher artifact(ArtifactMatcher artifactMatcher) {
        return new DependencyMatcher() {
            @Override
            public boolean test(Dependency dependency) {
                return artifactMatcher.test(dependency.getArtifact());
            }
        };
    }

    static DependencyMatcher scope(Collection<String> included, Collection<String> excluded) {
        HashSet<String> includedSet = included == null ? null : new HashSet<>(included);
        HashSet<String> excludedSet = included == null ? null : new HashSet<>(excluded);
        return new DependencyMatcher() {
            @Override
            public boolean test(Dependency dependency) {
                String scope = dependency.getScope();
                return (includedSet == null || includedSet.contains(scope))
                        && (excludedSet == null || !excludedSet.contains(scope));
            }
        };
    }

    static DependencyMatcher optional(boolean optional) {
        return new DependencyMatcher() {
            @Override
            public boolean test(Dependency dependency) {
                return dependency.isOptional() == optional;
            }
        };
    }

    static DependencyMatcher parse(String spec) {
        // TODO: do it
        throw new RuntimeException("not yet implemented");
    }
}
