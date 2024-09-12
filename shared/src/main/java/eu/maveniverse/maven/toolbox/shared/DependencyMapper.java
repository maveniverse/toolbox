/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

import eu.maveniverse.maven.toolbox.shared.internal.SpecParser;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import org.eclipse.aether.graph.Dependency;

/**
 * Mapper that maps dependencies to dependencies.
 *
 * @TODO scope, optional, exclusions
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
                default:
                    throw new IllegalArgumentException("unknown op " + node.getValue());
            }
        }

        public DependencyMapper build() {
            return build(DependencyMapper.class);
        }
    }
}
