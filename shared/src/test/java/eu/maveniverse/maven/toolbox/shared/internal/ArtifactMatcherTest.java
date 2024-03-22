/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.jupiter.api.Test;

/**
 * Mapper that maps artifact to artifact.
 */
public class ArtifactMatcherTest {
    private final Artifact artifact = new DefaultArtifact("g:a:v");

    @Test
    void any() {
        assertTrue(ArtifactMatcher.any().test(artifact));
        assertTrue(ArtifactMatcher.artifact("*").test(artifact));
    }

    @Test
    void unique() {
        ArtifactMatcher matcher = ArtifactMatcher.unique();
        assertTrue(matcher.test(artifact));
        assertFalse(matcher.test(artifact));
    }

    @Test
    void composedAnd() {
        assertTrue(ArtifactMatcher.and(
                        ArtifactMatcher.artifact("g:*:*"),
                        ArtifactMatcher.artifact("*:a:*"),
                        ArtifactMatcher.artifact("*:*:v"))
                .test(artifact));
    }

    @Test
    void composedOr() {
        assertTrue(ArtifactMatcher.or(
                        ArtifactMatcher.artifact("foo:*:*"),
                        ArtifactMatcher.artifact("*:foo:*"),
                        ArtifactMatcher.artifact("*:*:v"))
                .test(artifact));
    }

    @Test
    void startsWith() {
        assertTrue(ArtifactMatcher.artifact("g*:a*:v*").test(artifact));
    }

    @Test
    void endsWith() {
        assertTrue(ArtifactMatcher.artifact("*g:*a:*v").test(artifact));
    }
}
