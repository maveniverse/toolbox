/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.function.Function;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.jupiter.api.Test;

public class ArtifactMapperTest {
    @Test
    void noChangeEquals() {
        DefaultArtifact a = new DefaultArtifact("a:b:c");
        Artifact other = ArtifactMapper.baseVersion().apply(a);
        assertEquals(a, other);
    }

    @Test
    void allOfThemComposed() {
        Artifact mapped = ArtifactMapper.compose(
                        ArtifactMapper.baseVersion(),
                        ArtifactMapper.omitClassifier(),
                        ArtifactMapper.rename(s -> "g1", s -> "a1", Function.identity()))
                .apply(new DefaultArtifact("g:a:jar:classifier:1.0-20240322.090900-12"));
        assertEquals(new DefaultArtifact("g1:a1:1.0-SNAPSHOT"), mapped);
    }

    @Test
    void parse() {
        HashMap<String, Object> properties = new HashMap<>();
        properties.put("groupId", "org.some.group");
        Artifact artifact = new DefaultArtifact("g:a:jar:classifier:1.0-20240322.090900-12");

        Artifact mapped;

        mapped = ArtifactMapper.build(properties, "omitClassifier()").apply(artifact);
        assertEquals(new DefaultArtifact("g:a:jar:1.0-20240322.090900-12"), mapped);

        mapped = ArtifactMapper.build(properties, "baseVersion()").apply(artifact);
        assertEquals(new DefaultArtifact("g:a:jar:classifier:1.0-SNAPSHOT"), mapped);

        mapped = ArtifactMapper.build(properties, "compose(omitClassifier(), baseVersion())")
                .apply(artifact);
        assertEquals(new DefaultArtifact("g:a:jar:1.0-SNAPSHOT"), mapped);

        mapped = ArtifactMapper.build(properties, "rename(${groupId},*,1.0.0)")
                .apply(artifact);
        assertEquals(new DefaultArtifact("org.some.group:a:jar:classifier:1.0.0"), mapped);
    }
}
