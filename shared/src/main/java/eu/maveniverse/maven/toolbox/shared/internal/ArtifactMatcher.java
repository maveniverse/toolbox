/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.function.Predicate;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

/**
 * Simple artifact matcher. Supports {@code "*"} pattern as "any", and {@code "xxx*"} as "starts with" and
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

    static ArtifactMatcher unique() {
        HashSet<Artifact> artifacts = new HashSet<>();
        return new ArtifactMatcher() {
            @Override
            public boolean test(Artifact artifact) {
                return artifacts.add(artifact);
            }
        };
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
            return str.endsWith(pattern.substring(0, pattern.length() - 1));
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
