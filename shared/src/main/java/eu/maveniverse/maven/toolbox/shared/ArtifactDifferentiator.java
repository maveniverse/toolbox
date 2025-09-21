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

/**
 * Differentiators for artifacts: these assign some "facet" representing string to investigated artifact property
 * or attribute.
 * For example: "major version differentiator" may cut "major version" from artifact version, and differentiate
 * artifacts by major versions.
 */
public interface ArtifactDifferentiator extends Function<Artifact, String> {
    static ArtifactDifferentiator majorVersion() {
        return new ArtifactDifferentiator() {
            @Override
            public String apply(Artifact artifact) {
                String ver = artifact.getVersion();
                int firstDot = ver.indexOf('.');
                if (firstDot > 0) {
                    return ver.substring(0, firstDot);
                } else {
                    return artifact.getVersion();
                }
            }
        };
    }

    static ArtifactDifferentiator minorVersion() {
        return new ArtifactDifferentiator() {
            @Override
            public String apply(Artifact artifact) {
                String ver = artifact.getVersion();
                int firstDot = ver.indexOf('.');
                if (firstDot > 0) {
                    int secondDot = ver.indexOf('.', firstDot + 1);
                    if (secondDot > firstDot) {
                        return ver.substring(0, secondDot);
                    }
                }
                return artifact.getVersion();
            }
        };
    }

    static ArtifactDifferentiator baseVersion() {
        return new ArtifactDifferentiator() {
            @Override
            public String apply(Artifact artifact) {
                return artifact.getBaseVersion();
            }
        };
    }

    static ArtifactDifferentiator version() {
        return new ArtifactDifferentiator() {
            @Override
            public String apply(Artifact artifact) {
                return artifact.getVersion();
            }
        };
    }

    // java level?
    // jpms vs non-jpms?

    static ArtifactDifferentiator build(Map<String, ?> properties, String spec) {
        requireNonNull(properties, "properties");
        requireNonNull(spec, "spec");
        ArtifactKeyFactoryBuilder builder = new ArtifactKeyFactoryBuilder(properties);
        try {
            SpecParser.parse(spec).accept(builder);
            return builder.build();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid artifact differentiator spec: " + spec, e);
        }
    }

    class ArtifactKeyFactoryBuilder extends SpecParser.Builder {
        public ArtifactKeyFactoryBuilder(Map<String, ?> properties) {
            super(properties);
        }

        @Override
        protected void processOp(SpecParser.Node node) {
            switch (node.getValue()) {
                case "majorVersion": {
                    params.add(majorVersion());
                    break;
                }
                case "minorVersion": {
                    params.add(minorVersion());
                    break;
                }
                case "baseVersion": {
                    params.add(baseVersion());
                    break;
                }
                case "version": {
                    params.add(version());
                    break;
                }
                default:
                    throw new IllegalArgumentException("unknown op " + node.getValue());
            }
        }

        public ArtifactDifferentiator build() {
            return build(ArtifactDifferentiator.class);
        }
    }
}
