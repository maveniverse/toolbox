/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.jupiter.api.Test;

public class ArtifactMapperTest {
    @Test
    void identity() {
        DefaultArtifact a = new DefaultArtifact("a:b:c");
        Artifact other = ArtifactMapper.identity().map(a);
        assertEquals(a, other);
    }

    @Test
    void allOfThemComposed() {
        Artifact mapped = ArtifactMapper.compose(
                        ArtifactMapper.identity(),
                        ArtifactMapper.baseVersion(),
                        ArtifactMapper.omitClassifier(),
                        ArtifactMapper.rename("g1", "a1", null))
                .map(new DefaultArtifact("g:a:jar:classifier:1.0-20240322.090900-12"));
        assertEquals(new DefaultArtifact("g1:a1:1.0-SNAPSHOT"), mapped);
    }
}