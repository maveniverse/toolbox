/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

/**
 * Artifact matcher. Supports {@code "*"} pattern as "any", and {@code "xxx*"} as "starts with" and
 * {@code "*xxx"} as "ends with".
 */
public interface ArtifactMatcher extends Predicate<Artifact> {
    static ArtifactMatcher not(ArtifactMatcher matcher) {
        return new ArtifactMatcher() {
            @Override
            public boolean test(Artifact artifact) {
                return !matcher.test(artifact);
            }
        };
    }

    static ArtifactMatcher and(ArtifactMatcher... matchers) {
        return and(Arrays.asList(matchers));
    }

    static ArtifactMatcher and(Collection<ArtifactMatcher> matchers) {
        return new ArtifactMatcher() {
            @Override
            public boolean test(Artifact artifact) {
                for (ArtifactMatcher matcher : matchers) {
                    if (!matcher.test(artifact)) {
                        return false;
                    }
                }
                return true;
            }
        };
    }

    static ArtifactMatcher or(ArtifactMatcher... matchers) {
        return or(Arrays.asList(matchers));
    }

    static ArtifactMatcher or(Collection<ArtifactMatcher> matchers) {
        return new ArtifactMatcher() {
            @Override
            public boolean test(Artifact artifact) {
                for (ArtifactMatcher matcher : matchers) {
                    if (matcher.test(artifact)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    static ArtifactMatcher withoutClassifier() {
        return a -> a.getClassifier() == null || a.getClassifier().trim().isEmpty();
    }

    static ArtifactMatcher artifact(String coordinate) {
        Artifact prototype = parsePrototype(coordinate);
        return a -> matches(prototype.getGroupId(), a.getGroupId())
                && matches(prototype.getArtifactId(), a.getArtifactId())
                && matches(prototype.getVersion(), a.getVersion())
                && matches(prototype.getExtension(), a.getExtension())
                && matches(prototype.getClassifier(), a.getClassifier());
    }

    static ArtifactMatcher any() {
        return artifact("*");
    }

    static ArtifactMatcher snapshot() {
        return new ArtifactMatcher() {
            @Override
            public boolean test(Artifact artifact) {
                return artifact.isSnapshot();
            }
        };
    }

    static ArtifactMatcher unique() {
        return uniqueBy(ArtifactNameMapper.GACEVKey());
    }

    static ArtifactMatcher uniqueBy(ArtifactNameMapper mapper) {
        HashSet<String> keys = new HashSet<>();
        return new ArtifactMatcher() {
            @Override
            public boolean test(Artifact artifact) {
                return keys.add(mapper.apply(artifact));
            }
        };
    }

    static ArtifactMatcher build(Map<String, Object> properties, String spec) {
        requireNonNull(properties, "properties");
        requireNonNull(spec, "spec");
        ArtifactMatcherBuilder builder = new ArtifactMatcherBuilder(properties);
        SpecParser.parse(spec).accept(builder);
        return builder.build();
    }

    class ArtifactMatcherBuilder extends SpecParser.Builder {
        public ArtifactMatcherBuilder(Map<String, Object> properties) {
            super(properties);
        }

        @Override
        public boolean visitEnter(SpecParser.Node node) {
            return super.visitEnter(node) && !"uniqueBy".equals(node.getValue());
        }

        @Override
        protected void processOp(SpecParser.Node node) {
            switch (node.getValue()) {
                case "withoutClassifier": {
                    params.add(withoutClassifier());
                    break;
                }
                case "any": {
                    params.add(any());
                    break;
                }
                case "snapshot": {
                    params.add(snapshot());
                    break;
                }
                case "artifact": {
                    params.add(artifact(stringParam(node.getValue())));
                    break;
                }
                case "unique": {
                    params.add(unique());
                    break;
                }
                case "uniqueBy": {
                    if (node.getChildren().size() != 1) {
                        throw new IllegalArgumentException("op uniqueBy accepts only 1 argument");
                    }
                    ArtifactNameMapper.ArtifactNameMapperBuilder nameMapper =
                            new ArtifactNameMapper.ArtifactNameMapperBuilder(properties);
                    node.getChildren().get(0).accept(nameMapper);
                    params.add(uniqueBy(nameMapper.build()));
                    node.getChildren().clear();
                    break;
                }
                case "not": {
                    params.add(not(artifactMatcherParam(node.getValue())));
                    break;
                }
                case "and": {
                    params.add(and(artifactMatcherParams(node.getValue())));
                    break;
                }
                case "or": {
                    params.add(or(artifactMatcherParams(node.getValue())));
                    break;
                }
                default:
                    throw new IllegalArgumentException("unknown op " + node.getValue());
            }
        }

        private ArtifactMatcher artifactMatcherParam(String op) {
            if (params.isEmpty()) {
                throw new IllegalArgumentException("bad parameter count for " + op);
            }
            return (ArtifactMatcher) params.remove(params.size() - 1);
        }

        private List<ArtifactMatcher> artifactMatcherParams(String op) {
            ArrayList<ArtifactMatcher> result = new ArrayList<>();
            while (!params.isEmpty()) {
                if (params.get(params.size() - 1) instanceof ArtifactMatcher) {
                    result.add(artifactMatcherParam(op));
                } else {
                    break;
                }
            }
            return result;
        }

        public ArtifactMatcher build() {
            if (params.size() != 1) {
                throw new IllegalArgumentException("bad spec");
            }
            return (ArtifactMatcher) params.get(0);
        }
    }

    private static boolean isAny(String str) {
        return "*".equals(str);
    }

    private static boolean matches(String pattern, String str) {
        if (isAny(pattern)) {
            return true;
        } else if (pattern.endsWith("*")) {
            return str.startsWith(pattern.substring(0, pattern.length() - 1));
        } else if (pattern.startsWith("*")) {
            return str.endsWith(pattern.substring(1));
        } else {
            return Objects.equals(pattern, str);
        }
    }

    private static Artifact parsePrototype(String coordinate) {
        requireNonNull(coordinate, "coordinate");
        Artifact s;
        String[] parts = coordinate.split(":", -1);
        switch (parts.length) {
            case 1:
                s = new DefaultArtifact(parts[0], "*", "*", "*", "*");
                break;
            case 2:
                s = new DefaultArtifact(parts[0], parts[1], "*", "*", "*");
                break;
            case 3:
                s = new DefaultArtifact(parts[0], parts[1], "*", "*", parts[2]);
                break;
            case 4:
                s = new DefaultArtifact(parts[0], parts[1], "*", parts[2], parts[3]);
                break;
            case 5:
                s = new DefaultArtifact(parts[0], parts[1], parts[2], parts[3], parts[4]);
                break;
            default:
                throw new IllegalArgumentException("Bad artifact coordinates " + coordinate
                        + ", expected format is <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>");
        }
        return s;
    }
}
