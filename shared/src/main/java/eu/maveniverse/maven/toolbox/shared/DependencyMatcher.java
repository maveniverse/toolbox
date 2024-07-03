/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.toolbox.shared.internal.SpecParser;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Predicate;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.version.VersionScheme;

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

    static DependencyMatcher any() {
        return dependency -> true;
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
        HashSet<String> excludedSet = excluded == null ? null : new HashSet<>(excluded);
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

    static DependencyMatcher build(VersionScheme versionScheme, Map<String, ?> properties, String spec) {
        requireNonNull(properties, "properties");
        requireNonNull(spec, "spec");
        DependencyMatcherBuilder builder = new DependencyMatcherBuilder(versionScheme, properties);
        SpecParser.parse(spec).accept(builder);
        return builder.build();
    }

    class DependencyMatcherBuilder extends SpecParser.Builder {
        public DependencyMatcherBuilder(VersionScheme versionScheme, Map<String, ?> properties) {
            super(versionScheme, properties);
        }

        @Override
        public boolean visitEnter(SpecParser.Node node) {
            return super.visitEnter(node) && !"artifact".equals(node.getValue());
        }

        @Override
        protected void processOp(SpecParser.Node node) {
            switch (node.getValue()) {
                case "any": {
                    params.add(any());
                    break;
                }
                case "scopeIncluded": {
                    params.add(scope(stringParams(node.getValue()), null));
                    break;
                }
                case "scopeExcluded": {
                    params.add(scope(null, stringParams(node.getValue())));
                    break;
                }
                case "optional": {
                    params.add(optional(Boolean.parseBoolean(stringParam(node.getValue()))));
                    break;
                }
                case "artifact": {
                    if (node.getChildren().size() != 1) {
                        throw new IllegalArgumentException("op artifact accepts only 1 argument");
                    }
                    ArtifactMatcher.ArtifactMatcherBuilder matcher =
                            new ArtifactMatcher.ArtifactMatcherBuilder(versionScheme, properties);
                    node.accept(matcher);
                    params.add(artifact(matcher.build()));
                    node.getChildren().clear();
                    break;
                }
                case "not": {
                    params.add(not(typedParam(DependencyMatcher.class, node.getValue())));
                    break;
                }
                case "and": {
                    params.add(and(typedParams(DependencyMatcher.class, node.getValue())));
                    break;
                }
                case "or": {
                    params.add(or(typedParams(DependencyMatcher.class, node.getValue())));
                    break;
                }
                default:
                    throw new IllegalArgumentException("unknown op " + node.getValue());
            }
        }

        public DependencyMatcher build() {
            return build(DependencyMatcher.class);
        }
    }
}
