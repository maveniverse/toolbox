/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionScheme;
import org.junit.jupiter.api.Test;

public class ArtifactVersionSelectorTest {
    private final VersionScheme versionScheme = new GenericVersionScheme();

    private Version version(String version) {
        try {
            return versionScheme.parseVersion(version);
        } catch (InvalidVersionSpecificationException e) {
            throw new RuntimeException(e); // never in generic
        }
    }

    private final List<Version> versions = Arrays.asList(
            version("2.0.0"),
            version("3.0.1"),
            version("3.0.2-alpha"),
            version("3.1.0M1"),
            version("3.1.0"),
            version("3.1.1M1"),
            version("4.0.0RC"),
            version("4.0.0"));

    @Test
    void identity() {
        assertEquals("v1", ArtifactVersionSelector.identity().apply(new DefaultArtifact("g:a:v1"), versions));
        assertEquals(
                "v1", ArtifactVersionSelector.identity().apply(new DefaultArtifact("g:a:v1"), Collections.emptyList()));
    }

    @Test
    void last() {
        assertEquals("4.0.0", ArtifactVersionSelector.last().apply(new DefaultArtifact("g:a:v1"), versions));
        assertEquals(
                "v1", ArtifactVersionSelector.last().apply(new DefaultArtifact("g:a:v1"), Collections.emptyList()));
    }

    @Test
    void sameMajor() {
        assertEquals("3.1.1M1", ArtifactVersionSelector.major().apply(new DefaultArtifact("g:a:3.0.0"), versions));
        assertEquals(
                "v1", ArtifactVersionSelector.major().apply(new DefaultArtifact("g:a:v1"), Collections.emptyList()));
    }

    @Test
    void sameMinor() {
        assertEquals("3.0.2-alpha", ArtifactVersionSelector.minor().apply(new DefaultArtifact("g:a:3.0.0"), versions));
        assertEquals(
                "v1", ArtifactVersionSelector.minor().apply(new DefaultArtifact("g:a:v1"), Collections.emptyList()));
    }

    @Test
    void sameMajorNoPreviews() {
        assertEquals(
                "3.1.0",
                ArtifactVersionSelector.noPreviews(ArtifactVersionSelector.major())
                        .apply(new DefaultArtifact("g:a:3.0.0"), versions));
        assertEquals(
                "v1",
                ArtifactVersionSelector.noPreviews(ArtifactVersionSelector.major())
                        .apply(new DefaultArtifact("g:a:v1"), Collections.emptyList()));
    }

    @Test
    void sameMinorNoPreviews() {
        assertEquals(
                "3.0.1",
                ArtifactVersionSelector.noPreviews(ArtifactVersionSelector.minor())
                        .apply(new DefaultArtifact("g:a:3.0.0"), versions));
        assertEquals(
                "v1",
                ArtifactVersionSelector.noPreviews(ArtifactVersionSelector.minor())
                        .apply(new DefaultArtifact("g:a:v1"), Collections.emptyList()));
    }

    @Test
    void parse() {
        Artifact artifact = new DefaultArtifact("g:a:3.0.0");

        assertEquals(
                artifact.getVersion(),
                ArtifactVersionSelector.build(Collections.emptyMap(), "identity()")
                        .apply(artifact, versions));
        assertEquals(
                "4.0.0",
                ArtifactVersionSelector.build(Collections.emptyMap(), "last()").apply(artifact, versions));
        assertEquals(
                "3.1.1M1",
                ArtifactVersionSelector.build(Collections.emptyMap(), "major()").apply(artifact, versions));
        assertEquals(
                "3.0.2-alpha",
                ArtifactVersionSelector.build(Collections.emptyMap(), "minor()").apply(artifact, versions));
        assertEquals(
                "3.1.0",
                ArtifactVersionSelector.build(Collections.emptyMap(), "noPreviews(major())")
                        .apply(artifact, versions));
        assertEquals(
                "3.0.1",
                ArtifactVersionSelector.build(Collections.emptyMap(), "noPreviews(minor())")
                        .apply(artifact, versions));
    }
}
