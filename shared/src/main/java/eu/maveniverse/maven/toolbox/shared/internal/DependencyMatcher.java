/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.eclipse.aether.graph.Dependency;

import static java.util.Objects.requireNonNull;

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

    static DependencyMatcher build(Map<String, Object> properties, String spec) {
        requireNonNull(properties, "properties");
        requireNonNull(spec, "spec");
        DependencyMatcherBuilder builder = new DependencyMatcherBuilder(properties);
        SpecParser.parse(spec).accept(builder);
        return builder.build();
    }


    class DependencyMatcherBuilder extends SpecParser.Builder {
        public DependencyMatcherBuilder(Map<String, Object> properties) {
            super(properties);
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
                            new ArtifactMatcher.ArtifactMatcherBuilder(properties);
                    node.accept(matcher);
                    params.add(artifact(matcher.build()));
                    node.getChildren().clear();
                    break;
                }
                case "not": {
                    params.add(not(dependencyMatcherParam(node.getValue())));
                    break;
                }
                case "and": {
                    params.add(and(dependencyMatcherParams(node.getValue())));
                    break;
                }
                case "or": {
                    params.add(or(dependencyMatcherParams(node.getValue())));
                    break;
                }
                default:
                    throw new IllegalArgumentException("unknown op " + node.getValue());
            }
        }

        private DependencyMatcher dependencyMatcherParam(String op) {
            if (params.isEmpty()) {
                throw new IllegalArgumentException("bad parameter count for " + op);
            }
            return (DependencyMatcher) params.remove(params.size() - 1);
        }

        private List<DependencyMatcher> dependencyMatcherParams(String op) {
            ArrayList<DependencyMatcher> result = new ArrayList<>();
            while (!params.isEmpty()) {
                if (params.get(params.size() - 1) instanceof DependencyMatcher) {
                    result.add(dependencyMatcherParam(op));
                } else {
                    break;
                }
            }
            return result;
        }

        public DependencyMatcher build() {
            if (params.size() != 1) {
                throw new IllegalArgumentException("bad spec");
            }
            return (DependencyMatcher) params.get(0);
        }
    }
}
