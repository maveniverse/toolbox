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
            version("3.100.0"),
            version("3.101.0RC"),
            version("3.200.0-SNAPSHOT"),
            version("4.0.0RC"),
            version("4.0.0"),
            version("400.0.0"),
            version("401.0.0RC"),
            version("410.0.0-SNAPSHOT"));

    @Test
    void identity() {
        assertEquals("v1", ArtifactVersionSelector.identity().apply(new DefaultArtifact("g:a:v1"), versions));
        assertEquals(
                "v1", ArtifactVersionSelector.identity().apply(new DefaultArtifact("g:a:v1"), Collections.emptyList()));
    }

    @Test
    void last() {
        assertEquals("410.0.0-SNAPSHOT", ArtifactVersionSelector.last().apply(new DefaultArtifact("g:a:v1"), versions));
        assertEquals(
                "v1", ArtifactVersionSelector.last().apply(new DefaultArtifact("g:a:v1"), Collections.emptyList()));
    }

    @Test
    void prev() {
        assertEquals("3.1.0M1", ArtifactVersionSelector.prev().apply(new DefaultArtifact("g:a:3.1.0"), versions));
        assertEquals(
                "v1", ArtifactVersionSelector.prev().apply(new DefaultArtifact("g:a:v1"), Collections.emptyList()));
    }

    @Test
    void next() {
        assertEquals("3.0.2-alpha", ArtifactVersionSelector.next().apply(new DefaultArtifact("g:a:3.0.1"), versions));
        assertEquals(
                "v1", ArtifactVersionSelector.next().apply(new DefaultArtifact("g:a:v1"), Collections.emptyList()));
    }

    @Test
    void sameMajor() {
        assertEquals(
                "3.200.0-SNAPSHOT", ArtifactVersionSelector.major().apply(new DefaultArtifact("g:a:3.0.0"), versions));
        assertEquals("4.0.0", ArtifactVersionSelector.major().apply(new DefaultArtifact("g:a:4.0.0"), versions));
        assertEquals(
                "v1", ArtifactVersionSelector.major().apply(new DefaultArtifact("g:a:v1"), Collections.emptyList()));
    }

    @Test
    void sameMinor() {
        assertEquals("3.0.2-alpha", ArtifactVersionSelector.minor().apply(new DefaultArtifact("g:a:3.0.0"), versions));
        assertEquals("3.1.1M1", ArtifactVersionSelector.minor().apply(new DefaultArtifact("g:a:3.1.0"), versions));
        assertEquals(
                "v1", ArtifactVersionSelector.minor().apply(new DefaultArtifact("g:a:v1"), Collections.emptyList()));
    }

    @Test
    void sameMajorNoPreviews() {
        assertEquals(
                "3.200.0-SNAPSHOT",
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
    void contextualSnapshotsAndPreviews() {
        // contextually select next same major
        assertEquals(
                "3.100.0",
                ArtifactVersionSelector.contextualSnapshotsAndPreviews()
                        .apply(new DefaultArtifact("g:a:3.0.0"), versions));
        assertEquals(
                "3.200.0-SNAPSHOT",
                ArtifactVersionSelector.contextualSnapshotsAndPreviews()
                        .apply(new DefaultArtifact("g:a:3.0.0-SNAPSHOT"), versions));
        // no available versions; identity
        assertEquals(
                "v1",
                ArtifactVersionSelector.contextualSnapshotsAndPreviews()
                        .apply(new DefaultArtifact("g:a:v1"), Collections.emptyList()));
        assertEquals(
                "v1-SNAPSHOT",
                ArtifactVersionSelector.contextualSnapshotsAndPreviews()
                        .apply(new DefaultArtifact("g:a:v1-SNAPSHOT"), Collections.emptyList()));
        // parent POM like (plain integer counter)
        assertEquals(
                "3",
                ArtifactVersionSelector.contextualSnapshotsAndPreviews()
                        .apply(new DefaultArtifact("g:a:1"), Arrays.asList(version("1"), version("2"), version("3"))));
        assertEquals(
                "2",
                ArtifactVersionSelector.contextualSnapshotsAndPreviews()
                        .apply(
                                new DefaultArtifact("g:a:1"),
                                Arrays.asList(version("1"), version("2"), version("3-SNAPSHOT"))));
        assertEquals(
                "3",
                ArtifactVersionSelector.contextualSnapshotsAndPreviews()
                        .apply(
                                new DefaultArtifact("g:a:1-SNAPSHOT"),
                                Arrays.asList(version("1"), version("2"), version("3"))));
        assertEquals(
                "3-SNAPSHOT",
                ArtifactVersionSelector.contextualSnapshotsAndPreviews()
                        .apply(
                                new DefaultArtifact("g:a:1-SNAPSHOT"),
                                Arrays.asList(version("1"), version("2"), version("3-SNAPSHOT"))));
    }

    @Test
    void parse() {
        Artifact artifact = new DefaultArtifact("g:a:3.0.0");

        assertEquals(
                artifact.getVersion(),
                ArtifactVersionSelector.build(versionScheme, Collections.emptyMap(), "identity()")
                        .apply(artifact, versions));
        assertEquals(
                "2.0.0",
                ArtifactVersionSelector.build(versionScheme, Collections.emptyMap(), "first()")
                        .apply(artifact, versions));
        assertEquals(
                "410.0.0-SNAPSHOT",
                ArtifactVersionSelector.build(versionScheme, Collections.emptyMap(), "last()")
                        .apply(artifact, versions));
        assertEquals(
                "3.200.0-SNAPSHOT",
                ArtifactVersionSelector.build(versionScheme, Collections.emptyMap(), "major()")
                        .apply(artifact, versions));
        assertEquals(
                "3.0.2-alpha",
                ArtifactVersionSelector.build(versionScheme, Collections.emptyMap(), "minor()")
                        .apply(artifact, versions));
        assertEquals(
                "3.200.0-SNAPSHOT",
                ArtifactVersionSelector.build(versionScheme, Collections.emptyMap(), "noPreviews(major())")
                        .apply(artifact, versions));
        assertEquals(
                "3.0.1",
                ArtifactVersionSelector.build(versionScheme, Collections.emptyMap(), "noPreviews(minor())")
                        .apply(artifact, versions));
        assertEquals(
                "3.0.1",
                ArtifactVersionSelector.build(versionScheme, Collections.emptyMap(), "filtered(eq(3.0.1), last())")
                        .apply(artifact, versions));
        assertEquals(
                "400.0.0",
                ArtifactVersionSelector.build(versionScheme, Collections.emptyMap(), "noSnapshotsAndPreviews(last())")
                        .apply(artifact, versions));
        assertEquals(
                "3.100.0",
                ArtifactVersionSelector.build(versionScheme, Collections.emptyMap(), "noSnapshotsAndPreviews(major())")
                        .apply(artifact, versions));

        assertEquals(
                "401.0.0RC",
                ArtifactVersionSelector.build(
                                versionScheme, Collections.emptyMap(), "contextualSnapshotsAndPreviews(last())")
                        .apply(artifact.setVersion("3.0.0-rc-1"), versions));
        assertEquals(
                "410.0.0-SNAPSHOT",
                ArtifactVersionSelector.build(
                                versionScheme, Collections.emptyMap(), "contextualSnapshotsAndPreviews(last())")
                        .apply(artifact.setVersion("3.0.0-SNAPSHOT"), versions));

        assertEquals(
                "3.101.0RC",
                ArtifactVersionSelector.build(versionScheme, Collections.emptyMap(), "contextualSnapshotsAndPreviews()")
                        .apply(artifact.setVersion("3.0.0-rc-1"), versions));
        assertEquals(
                "3.200.0-SNAPSHOT",
                ArtifactVersionSelector.build(versionScheme, Collections.emptyMap(), "contextualSnapshotsAndPreviews()")
                        .apply(artifact.setVersion("3.0.0-SNAPSHOT"), versions));
    }
}
