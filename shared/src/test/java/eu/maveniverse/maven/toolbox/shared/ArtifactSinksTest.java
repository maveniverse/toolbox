/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.HashMap;
import org.junit.jupiter.api.Test;

public class ArtifactSinksTest {
    @Test
    void parse() {
        HashMap<String, Object> properties = new HashMap<>();
        properties.put("groupId", "org.some.group");
        Output output = new NullOutput();
        ArtifactSink artifactSink;

        artifactSink = ArtifactSinks.build(properties, output, "null()");
        assertInstanceOf(ArtifactSinks.NullArtifactSink.class, artifactSink);

        artifactSink = ArtifactSinks.build(properties, output, "matching(any(),null())");
        assertInstanceOf(ArtifactSinks.MatchingArtifactSink.class, artifactSink);

        artifactSink = ArtifactSinks.build(properties, output, "mapping(baseVersion(), matching(any(),null()))");
        assertInstanceOf(ArtifactSinks.MappingArtifactSink.class, artifactSink);
    }
}
