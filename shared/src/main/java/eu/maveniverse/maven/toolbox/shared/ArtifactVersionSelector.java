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
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.version.Version;

/**
 * Selector that selects artifact version. The <em>assumption is that {@code List<Version>} is sorted ascending</em>
 * (same way as resolver sorts versions).
 */
public interface ArtifactVersionSelector extends BiFunction<Artifact, List<Version>, String> {
    /**
     * Selector that returns artifact version.
     */
    static ArtifactVersionSelector identity() {
        return new ArtifactVersionSelector() {
            @Override
            public String apply(Artifact artifact, List<Version> versions) {
                return artifact.getVersion();
            }
        };
    }

    /**
     * Selector that return last version.
     */
    static ArtifactVersionSelector last() {
        return new ArtifactVersionSelector() {
            @Override
            public String apply(Artifact artifact, List<Version> versions) {
                return versions.isEmpty()
                        ? artifact.getVersion()
                        : versions.get(versions.size() - 1).toString();
            }
        };
    }

    /**
     * Selector that return last version with same "major" as artifact.
     */
    static ArtifactVersionSelector major() {
        return new ArtifactVersionSelector() {
            @Override
            public String apply(Artifact artifact, List<Version> versions) {
                int firstDot = artifact.getVersion().indexOf('.');
                if (firstDot > 0) {
                    String prefix = artifact.getVersion().substring(0, firstDot);
                    for (int i = versions.size() - 1; i >= 0; i--) {
                        String version = versions.get(i).toString();
                        if (version.startsWith(prefix)) {
                            return version;
                        }
                    }
                }
                return artifact.getVersion();
            }
        };
    }

    /**
     * Selector that return last version with same "minor" as artifact.
     */
    static ArtifactVersionSelector minor() {
        return new ArtifactVersionSelector() {
            @Override
            public String apply(Artifact artifact, List<Version> versions) {
                int firstDot = artifact.getVersion().indexOf('.');
                if (firstDot > 0) {
                    int secondDot = artifact.getVersion().indexOf('.', firstDot + 1);
                    if (secondDot > firstDot) {
                        String prefix = artifact.getVersion().substring(0, secondDot);
                        for (int i = versions.size() - 1; i >= 0; i--) {
                            String version = versions.get(i).toString();
                            if (version.startsWith(prefix)) {
                                return version;
                            }
                        }
                    }
                }
                return artifact.getVersion();
            }
        };
    }

    static ArtifactVersionSelector build(Map<String, ?> properties, String spec) {
        requireNonNull(properties, "properties");
        requireNonNull(spec, "spec");
        ArtifactVersionSelector.ArtifactVersionSelectorBuilder builder =
                new ArtifactVersionSelector.ArtifactVersionSelectorBuilder(properties);
        SpecParser.parse(spec).accept(builder);
        return builder.build();
    }

    class ArtifactVersionSelectorBuilder extends SpecParser.Builder {
        public ArtifactVersionSelectorBuilder(Map<String, ?> properties) {
            super(properties);
        }

        @Override
        protected void processOp(SpecParser.Node node) {
            switch (node.getValue()) {
                case "identity":
                    params.add(identity());
                    break;
                case "last":
                    params.add(last());
                    break;
                case "major":
                    params.add(major());
                    break;
                case "minor":
                    params.add(minor());
                    break;
                default:
                    throw new IllegalArgumentException("unknown op " + node.getValue());
            }
        }

        public ArtifactVersionSelector build() {
            if (params.size() != 1) {
                throw new IllegalArgumentException("bad spec");
            }
            return (ArtifactVersionSelector) params.get(0);
        }
    }
}
