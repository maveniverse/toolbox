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

public class ArtifactNameMapperTest {
    private final Artifact artifact = new DefaultArtifact("g:a:v");

    @Test
    void compose() {
        ArtifactNameMapper mapper =
                ArtifactNameMapper.compose(ArtifactNameMapper.prefix("foo/"), ArtifactNameMapper.ACVE());
        assertEquals(mapper.map(artifact), "foo/a-v.jar");
    }
}
