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
import java.util.Map;
import java.util.function.Function;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.Exclusion;

/**
 * Mapper that maps dependencies to dependencies.
 */
public interface DependencyMapper extends Function<Dependency, Dependency> {
    static DependencyMapper compose(DependencyMapper... mappers) {
        return compose(Arrays.asList(mappers));
    }

    static DependencyMapper compose(Collection<DependencyMapper> mappers) {
        return new DependencyMapper() {
            @Override
            public Dependency apply(Dependency dependency) {
                for (DependencyMapper mapper : mappers) {
                    dependency = mapper.apply(dependency);
                }
                return dependency;
            }
        };
    }

    static DependencyMapper identity() {
        return new DependencyMapper() {
            @Override
            public Dependency apply(Dependency dependency) {
                return dependency;
            }
        };
    }

    static DependencyMapper optional(boolean optional) {
        return new DependencyMapper() {
            @Override
            public Dependency apply(Dependency dependency) {
                return dependency.setOptional(optional);
            }
        };
    }

    static DependencyMapper scope(String scope) {
        return new DependencyMapper() {
            @Override
            public Dependency apply(Dependency dependency) {
                return dependency.setScope(scope);
            }
        };
    }

    static DependencyMapper exclusion(Collection<String> exclusions) {
        requireNonNull(exclusions, "exclusions");
        return new DependencyMapper() {
            @Override
            public Dependency apply(Dependency dependency) {
                return dependency.setExclusions(exclusions.stream()
                        .map(a -> new DefaultArtifact(a + ":irrelevant"))
                        .map(a -> new Exclusion(a.getGroupId(), a.getArtifactId(), a.getClassifier(), a.getExtension()))
                        .toList());
            }
        };
    }

    class DependencyMapperBuilder extends SpecParser.Builder {
        public DependencyMapperBuilder(Map<String, ?> properties) {
            super(properties);
        }

        @Override
        protected void processOp(SpecParser.Node node) {
            switch (node.getValue()) {
                case "identity": {
                    params.add(identity());
                    break;
                }
                case "compose": {
                    params.add(compose(typedParams(DependencyMapper.class, node.getValue())));
                    break;
                }
                case "optional": {
                    params.add(optional(booleanParam(node.getValue())));
                    break;
                }
                case "scope": {
                    params.add(scope(stringParam(node.getValue())));
                    break;
                }
                case "exclusion": {
                    params.add(exclusion(stringParams(node.getValue())));
                    break;
                }
                default:
                    throw new IllegalArgumentException("unknown op " + node.getValue());
            }
        }

        public DependencyMapper build() {
            return build(DependencyMapper.class);
        }
    }
}
