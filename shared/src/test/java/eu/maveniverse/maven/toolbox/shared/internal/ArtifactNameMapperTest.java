/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.jupiter.api.Test;

public class ArtifactNameMapperTest {
    private final Artifact artifact = new DefaultArtifact("g:a:jar:classifier:1.0-20240322.113300-2");

    @Test
    void compose() {
        ArtifactNameMapper mapper =
                ArtifactNameMapper.compose(ArtifactNameMapper.fixed("foo/"), ArtifactNameMapper.AbVE());
        assertEquals("foo/a-1.0-SNAPSHOT.jar", mapper.apply(artifact));
    }

    @Test
    void composeInsane() {
        ArtifactNameMapper mapper = ArtifactNameMapper.compose(
                ArtifactNameMapper.fixed("lib/"),
                ArtifactNameMapper.G(),
                ArtifactNameMapper.fixed("/"),
                ArtifactNameMapper.ACVE());
        assertEquals("lib/g/a-classifier-1.0-20240322.113300-2.jar", mapper.apply(artifact));
    }

    @Test
    void parse() {
        ArtifactNameMapper artifactNameMapper;
        String mapped;

        artifactNameMapper = ArtifactNameMapper.build(Collections.emptyMap(), "fixed(zoo)");
        mapped = artifactNameMapper.apply(artifact);
        assertEquals("zoo", mapped);

        artifactNameMapper =
                ArtifactNameMapper.build(Collections.emptyMap(), "compose(G(), fixed(:), A(), fixed(:), V())");
        mapped = artifactNameMapper.apply(artifact);
        assertEquals("g:a:1.0-20240322.113300-2", mapped);

        artifactNameMapper = ArtifactNameMapper.build(Collections.emptyMap(), "repositoryDefault()");
        mapped = artifactNameMapper.apply(artifact);
        assertEquals("g/a/1.0-SNAPSHOT/a-1.0-20240322.113300-2-classifier.jar", mapped);

        HashMap<String, Object> properties = new HashMap<>();
        properties.put("fs", "--this-is-a-separator--");
        artifactNameMapper = ArtifactNameMapper.build(properties, "repository(${fs})");
        mapped = artifactNameMapper.apply(artifact);
        assertEquals(
                "g--this-is-a-separator--a--this-is-a-separator--1.0-SNAPSHOT--this-is-a-separator--a-1.0-20240322.113300-2-classifier.jar",
                mapped);

        artifactNameMapper = ArtifactNameMapper.build(Collections.emptyMap(), "GAKey()");
        mapped = artifactNameMapper.apply(artifact);
        assertEquals("g:a", mapped);

        artifactNameMapper = ArtifactNameMapper.build(Collections.emptyMap(), "GAVKey()");
        mapped = artifactNameMapper.apply(artifact);
        assertEquals("g:a:1.0-20240322.113300-2", mapped);

        artifactNameMapper = ArtifactNameMapper.build(Collections.emptyMap(), "GAbVKey()");
        mapped = artifactNameMapper.apply(artifact);
        assertEquals("g:a:1.0-SNAPSHOT", mapped);

        artifactNameMapper = ArtifactNameMapper.build(Collections.emptyMap(), "GACEVKey()");
        mapped = artifactNameMapper.apply(artifact);
        assertEquals(artifact.toString(), mapped);
    }
}
