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

import java.util.HashMap;
import java.util.Map;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.jupiter.api.Test;

public class ArtifactMatcherTest {
    private final Artifact artifact = new DefaultArtifact("g:a:classifier:jar:1.0-20240322.113300-2");

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
                        ArtifactMatcher.artifact("*:*:1*"))
                .test(artifact));
    }

    @Test
    void composedNotAnd() {
        assertFalse(ArtifactMatcher.not(ArtifactMatcher.and(
                        ArtifactMatcher.artifact("g:*:*"),
                        ArtifactMatcher.artifact("*:a:*"),
                        ArtifactMatcher.artifact("*:*:1*")))
                .test(artifact));
    }

    @Test
    void composedOr() {
        assertTrue(ArtifactMatcher.or(
                        ArtifactMatcher.artifact("foo:*:*"),
                        ArtifactMatcher.artifact("*:foo:*"),
                        ArtifactMatcher.artifact("*:*:1*"))
                .test(artifact));
    }

    @Test
    void composedNotOr() {
        assertFalse(ArtifactMatcher.not(ArtifactMatcher.or(
                        ArtifactMatcher.artifact("foo:*:*"),
                        ArtifactMatcher.artifact("*:foo:*"),
                        ArtifactMatcher.artifact("*:*:1*")))
                .test(artifact));
    }

    @Test
    void startsWith() {
        assertTrue(ArtifactMatcher.artifact("g*:a*:1*").test(artifact));
    }

    @Test
    void endsWith() {
        assertTrue(ArtifactMatcher.artifact("*g:*a:*-2").test(artifact));
    }

    @Test
    void parse() {
        Artifact artifact1 = new DefaultArtifact("g1:a:zip:classifier:1.0-20240322.113300-2");
        Artifact artifact2 = new DefaultArtifact("g2:a:jar:1.0-20240322.113300-3");
        Artifact artifact3 = new DefaultArtifact("g2:a:jar:1.1");

        Map<String, Object> properties = new HashMap<>();
        properties.put("groupId", "g2");
        ArtifactMatcher parsed;

        parsed = ArtifactMatcher.build(properties, "any()");
        assertTrue(parsed.test(artifact1));
        assertTrue(parsed.test(artifact2));
        assertTrue(parsed.test(artifact3));

        parsed = ArtifactMatcher.build(properties, "not(any())");
        assertFalse(parsed.test(artifact1));
        assertFalse(parsed.test(artifact2));
        assertFalse(parsed.test(artifact3));

        parsed = ArtifactMatcher.build(properties, "or(not(any()),any())");
        assertTrue(parsed.test(artifact1));
        assertTrue(parsed.test(artifact2));
        assertTrue(parsed.test(artifact3));

        parsed = ArtifactMatcher.build(properties, "withoutClassifier()");
        assertFalse(parsed.test(artifact1));
        assertTrue(parsed.test(artifact2));
        assertTrue(parsed.test(artifact3));

        parsed = ArtifactMatcher.build(properties, "snapshot()");
        assertTrue(parsed.test(artifact1));
        assertTrue(parsed.test(artifact2));
        assertFalse(parsed.test(artifact3));

        parsed = ArtifactMatcher.build(properties, "artifact(*)");
        assertTrue(parsed.test(artifact1));
        assertTrue(parsed.test(artifact2));
        assertTrue(parsed.test(artifact3));

        parsed = ArtifactMatcher.build(properties, "artifact(*:a:*)");
        assertTrue(parsed.test(artifact1));
        assertTrue(parsed.test(artifact2));
        assertTrue(parsed.test(artifact3));

        parsed = ArtifactMatcher.build(properties, "artifact(*:*:*:jar:*)");
        assertFalse(parsed.test(artifact1));
        assertTrue(parsed.test(artifact2));
        assertTrue(parsed.test(artifact3));

        parsed = ArtifactMatcher.build(properties, "artifact(g1)");
        assertTrue(parsed.test(artifact1));
        assertFalse(parsed.test(artifact2));
        assertFalse(parsed.test(artifact3));

        parsed = ArtifactMatcher.build(properties, "artifact(${groupId})");
        assertFalse(parsed.test(artifact1));
        assertTrue(parsed.test(artifact2));
        assertTrue(parsed.test(artifact3));

        parsed = ArtifactMatcher.build(properties, "unique()");
        assertTrue(parsed.test(artifact1));
        assertTrue(parsed.test(artifact2));
        assertTrue(parsed.test(artifact3));
        assertFalse(parsed.test(artifact1));
        assertFalse(parsed.test(artifact2));
        assertFalse(parsed.test(artifact3));

        parsed = ArtifactMatcher.build(properties, "uniqueBy(GAKey())");
        assertTrue(parsed.test(artifact1));
        assertTrue(parsed.test(artifact2));
        assertFalse(parsed.test(artifact3));
        assertFalse(parsed.test(artifact1));
        assertFalse(parsed.test(artifact2));
        assertFalse(parsed.test(artifact3));

        parsed = ArtifactMatcher.build(properties, "uniqueBy(G())");
        assertTrue(parsed.test(artifact1));
        assertTrue(parsed.test(artifact2));
        assertFalse(parsed.test(artifact3));
        assertFalse(parsed.test(artifact1));
        assertFalse(parsed.test(artifact2));
        assertFalse(parsed.test(artifact3));

        parsed = ArtifactMatcher.build(properties, "uniqueBy(GACEVKey())");
        assertTrue(parsed.test(artifact1));
        assertTrue(parsed.test(artifact2));
        assertTrue(parsed.test(artifact3));
        assertFalse(parsed.test(artifact1));
        assertFalse(parsed.test(artifact2));
        assertFalse(parsed.test(artifact3));
    }
}
