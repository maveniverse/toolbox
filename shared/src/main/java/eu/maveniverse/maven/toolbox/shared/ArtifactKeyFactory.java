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
import java.util.Map;
import java.util.function.Function;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;

/**
 * Key factory for artifacts.
 */
public interface ArtifactKeyFactory extends Function<Artifact, String> {
    static ArtifactKeyFactory id() {
        return new ArtifactKeyFactory() {
            @Override
            public String apply(Artifact artifact) {
                return ArtifactIdUtils.toId(artifact);
            }
        };
    }

    static ArtifactKeyFactory baseId() {
        return new ArtifactKeyFactory() {
            @Override
            public String apply(Artifact artifact) {
                return ArtifactIdUtils.toBaseId(artifact);
            }
        };
    }

    static ArtifactKeyFactory versionlessId() {
        return new ArtifactKeyFactory() {
            @Override
            public String apply(Artifact artifact) {
                return ArtifactIdUtils.toVersionlessId(artifact);
            }
        };
    }

    static ArtifactKeyFactory ga() {
        return new ArtifactKeyFactory() {
            @Override
            public String apply(Artifact artifact) {
                return artifact.getGroupId() + ":" + artifact.getArtifactId();
            }
        };
    }

    static ArtifactKeyFactory build(Map<String, ?> properties, String spec) {
        requireNonNull(properties, "properties");
        requireNonNull(spec, "spec");
        ArtifactKeyFactoryBuilder builder = new ArtifactKeyFactoryBuilder(properties);
        try {
            SpecParser.parse(spec).accept(builder);
            return builder.build();
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid artifact key factory spec:" + spec, e);
        }
    }

    class ArtifactKeyFactoryBuilder extends SpecParser.Builder {
        public ArtifactKeyFactoryBuilder(Map<String, ?> properties) {
            super(properties);
        }

        @Override
        protected void processOp(SpecParser.Node node) {
            switch (node.getValue()) {
                case "id": {
                    params.add(id());
                    break;
                }
                case "baseId": {
                    params.add(baseId());
                    break;
                }
                case "versionlessId": {
                    params.add(versionlessId());
                    break;
                }
                case "ga": {
                    params.add(ga());
                    break;
                }
                default:
                    throw new IllegalArgumentException("unknown op " + node.getValue());
            }
        }

        public ArtifactKeyFactory build() {
            return build(ArtifactKeyFactory.class);
        }
    }
}
