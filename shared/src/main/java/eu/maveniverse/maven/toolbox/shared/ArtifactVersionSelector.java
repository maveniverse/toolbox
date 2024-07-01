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
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.version.Version;

/**
 * Selector that selects artifact version. The <em>assumption is that {@code List<Version>} is sorted ascending</em>
 * (same way as resolver sorts versions).
 */
public interface ArtifactVersionSelector extends BiFunction<Artifact, List<Version>, String> {
    /**
     * Selector that returns artifact version. This is the "fallback" selector as well.
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
                        ? identity().apply(artifact, versions)
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
                String ver = artifact.getVersion();
                int firstDot = ver.indexOf('.');
                if (firstDot > 0) {
                    String prefix = ver.substring(0, firstDot);
                    for (int i = versions.size() - 1; i >= 0; i--) {
                        String version = versions.get(i).toString();
                        if (version.startsWith(prefix)) {
                            return version;
                        }
                    }
                }
                return identity().apply(artifact, versions);
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
                String ver = artifact.getVersion();
                int firstDot = ver.indexOf('.');
                if (firstDot > 0) {
                    int secondDot = ver.indexOf('.', firstDot + 1);
                    if (secondDot > firstDot) {
                        String prefix = ver.substring(0, secondDot);
                        for (int i = versions.size() - 1; i >= 0; i--) {
                            String version = versions.get(i).toString();
                            if (version.startsWith(prefix)) {
                                return version;
                            }
                        }
                    }
                }
                return identity().apply(artifact, versions);
            }
        };
    }

    /**
     * A version selector that filters the candidates out of version list and applies delegated selector.
     */
    static ArtifactVersionSelector filtered(Predicate<Version> filter, ArtifactVersionSelector selector) {
        requireNonNull(filter, "filter");
        requireNonNull(selector, "selector");
        return new ArtifactVersionSelector() {
            @Override
            public String apply(Artifact artifact, List<Version> versions) {
                return selector.apply(artifact, versions.stream().filter(filter).collect(Collectors.toList()));
            }
        };
    }

    /**
     * A version selector that prevents selection of "preview" versions. Shorthand for "filtered(not(preview()), $selector)".
     */
    static ArtifactVersionSelector noPreviews(ArtifactVersionSelector selector) {
        return filtered(ArtifactVersionMatcher.not(ArtifactVersionMatcher.preview()), selector);
    }

    /**
     * A version selector that prevents selection of "snapshot" versions. Shorthand for "filtered(not(snapshot()), $selector)".
     */
    static ArtifactVersionSelector noSnapshots(ArtifactVersionSelector selector) {
        return filtered(ArtifactVersionMatcher.not(ArtifactVersionMatcher.snapshot()), selector);
    }

    /**
     * A version selector that prevents selection of "snapshot" and "preview" versions. Shorthand for "filtered(and(not(snapshot()), not(preview()), $selector)".
     */
    static ArtifactVersionSelector noSnapshotsAndPreviews(ArtifactVersionSelector selector) {
        return filtered(
                ArtifactVersionMatcher.and(
                        ArtifactVersionMatcher.not(ArtifactVersionMatcher.snapshot()),
                        ArtifactVersionMatcher.not(ArtifactVersionMatcher.preview())),
                selector);
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
        public boolean visitEnter(SpecParser.Node node) {
            return super.visitEnter(node) && !"filtered".equals(node.getValue());
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
                case "filtered":
                    if (node.getChildren().size() != 2) {
                        throw new IllegalArgumentException("op filtered accepts only 2 argument");
                    }
                    ArtifactVersionSelector.ArtifactVersionSelectorBuilder selectorBuilder =
                            new ArtifactVersionSelector.ArtifactVersionSelectorBuilder(properties);
                    node.getChildren().get(1).accept(selectorBuilder);
                    ArtifactVersionSelector selector = selectorBuilder.build();
                    ArtifactVersionMatcher.ArtifactVersionMatcherBuilder matcherBuilder =
                            new ArtifactVersionMatcher.ArtifactVersionMatcherBuilder(properties);
                    node.getChildren().get(0).accept(matcherBuilder);
                    ArtifactVersionMatcher matcher = matcherBuilder.build();
                    node.getChildren().clear();
                    params.add(filtered(matcher, selector));
                    break;
                case "noPreviews":
                    params.add(noPreviews(typedParam(ArtifactVersionSelector.class, node.getValue())));
                    break;
                case "noSnapshots":
                    params.add(noSnapshots(typedParam(ArtifactVersionSelector.class, node.getValue())));
                    break;
                case "noSnapshotsAndPreviews":
                    params.add(noSnapshotsAndPreviews(typedParam(ArtifactVersionSelector.class, node.getValue())));
                    break;
                default:
                    throw new IllegalArgumentException("unknown op " + node.getValue());
            }
        }

        public ArtifactVersionSelector build() {
            return build(ArtifactVersionSelector.class);
        }
    }
}
