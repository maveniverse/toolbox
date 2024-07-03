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
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

/**
 * Mapper that maps artifact to artifact.
 */
public interface ArtifactMapper extends Function<Artifact, Artifact> {
    static ArtifactMapper compose(ArtifactMapper... mappers) {
        return compose(Arrays.asList(mappers));
    }

    static ArtifactMapper compose(Collection<ArtifactMapper> mappers) {
        return new ArtifactMapper() {
            @Override
            public Artifact apply(Artifact artifact) {
                for (ArtifactMapper mapper : mappers) {
                    artifact = mapper.apply(artifact);
                }
                return artifact;
            }
        };
    }

    static ArtifactMapper identity() {
        return new ArtifactMapper() {
            @Override
            public Artifact apply(Artifact artifact) {
                return artifact;
            }
        };
    }

    static ArtifactMapper versionSuffix(String suffix) {
        if (suffix == null || suffix.isEmpty()) {
            throw new IllegalArgumentException("suffix must not be null or empty");
        }
        return new ArtifactMapper() {
            @Override
            public Artifact apply(Artifact artifact) {
                return new DefaultArtifact(
                        artifact.getGroupId(),
                        artifact.getArtifactId(),
                        artifact.getClassifier(),
                        artifact.getExtension(),
                        artifact.getVersion() + suffix,
                        artifact.getProperties(),
                        artifact.getFile());
            }
        };
    }

    static ArtifactMapper baseVersion() {
        return new ArtifactMapper() {
            @Override
            public Artifact apply(Artifact artifact) {
                return new DefaultArtifact(
                        artifact.getGroupId(),
                        artifact.getArtifactId(),
                        artifact.getClassifier(),
                        artifact.getExtension(),
                        artifact.getBaseVersion(),
                        artifact.getProperties(),
                        artifact.getFile());
            }
        };
    }

    static ArtifactMapper baseVersionSuffix(String suffix) {
        if (suffix == null || suffix.isEmpty()) {
            throw new IllegalArgumentException("suffix must not be null or empty");
        }
        return new ArtifactMapper() {
            @Override
            public Artifact apply(Artifact artifact) {
                return new DefaultArtifact(
                        artifact.getGroupId(),
                        artifact.getArtifactId(),
                        artifact.getClassifier(),
                        artifact.getExtension(),
                        artifact.getBaseVersion() + suffix,
                        artifact.getProperties(),
                        artifact.getFile());
            }
        };
    }

    static ArtifactMapper omitClassifier() {
        return new ArtifactMapper() {
            @Override
            public Artifact apply(Artifact artifact) {
                return new DefaultArtifact(
                        artifact.getGroupId(),
                        artifact.getArtifactId(),
                        null,
                        artifact.getExtension(),
                        artifact.getVersion(),
                        artifact.getProperties(),
                        artifact.getFile());
            }
        };
    }

    static ArtifactMapper rename(String g, String a, String v) {
        return new ArtifactMapper() {
            @Override
            public Artifact apply(Artifact artifact) {
                return new DefaultArtifact(
                        g != null ? g : artifact.getGroupId(),
                        a != null ? a : artifact.getArtifactId(),
                        artifact.getClassifier(),
                        artifact.getExtension(),
                        v != null ? v : artifact.getVersion(),
                        artifact.getProperties(),
                        artifact.getFile());
            }
        };
    }

    static ArtifactMapper build(Map<String, ?> properties, String spec) {
        requireNonNull(properties, "properties");
        requireNonNull(spec, "spec");
        ArtifactMapperBuilder builder = new ArtifactMapperBuilder(properties);
        SpecParser.parse(spec).accept(builder);
        return builder.build();
    }

    class ArtifactMapperBuilder extends SpecParser.Builder {
        public ArtifactMapperBuilder(Map<String, ?> properties) {
            super(properties);
        }

        @Override
        protected void processOp(SpecParser.Node node) {
            switch (node.getValue()) {
                case "identity": {
                    params.add(identity());
                    break;
                }
                case "baseVersion": {
                    params.add(baseVersion());
                    break;
                }
                case "versionSuffix": {
                    String p0 = stringParam(node.getValue());
                    params.add(versionSuffix(p0));
                    break;
                }
                case "baseVersionSuffix": {
                    String p0 = stringParam(node.getValue());
                    params.add(baseVersionSuffix(p0));
                    break;
                }
                case "omitClassifier": {
                    params.add(omitClassifier());
                    break;
                }
                case "rename": {
                    String p2 = stringParam(node.getValue());
                    String p1 = stringParam(node.getValue());
                    String p0 = stringParam(node.getValue());
                    params.add(rename(p0, p1, p2));
                    break;
                }
                case "compose": {
                    params.add(compose(typedParams(ArtifactMapper.class, node.getValue())));
                    break;
                }
                default:
                    throw new IllegalArgumentException("unknown op " + node.getValue());
            }
        }

        public ArtifactMapper build() {
            return build(ArtifactMapper.class);
        }
    }
}
