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
import java.util.stream.Collectors;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionScheme;
import org.junit.jupiter.api.Test;

public class ArtifactVersionMatcherTest {
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
    void parse() {
        assertEquals(
                versions,
                versions.stream()
                        .filter(ArtifactVersionMatcher.build(Collections.emptyMap(), "any()"))
                        .collect(Collectors.toList()));

        assertEquals(
                Arrays.asList(version("2.0.0"), version("3.0.1"), version("3.1.0"), version("4.0.0")),
                versions.stream()
                        .filter(ArtifactVersionMatcher.build(Collections.emptyMap(), "noPreviews()"))
                        .collect(Collectors.toList()));

        assertEquals(
                Arrays.asList(version("3.0.2-alpha"), version("3.1.0M1"), version("3.1.1M1"), version("4.0.0RC")),
                versions.stream()
                        .filter(ArtifactVersionMatcher.build(Collections.emptyMap(), "not(noPreviews())"))
                        .collect(Collectors.toList()));

        assertEquals(
                Collections.emptyList(),
                versions.stream()
                        .filter(ArtifactVersionMatcher.build(
                                Collections.emptyMap(), "and(not(noPreviews()),noPreviews())"))
                        .collect(Collectors.toList()));

        assertEquals(
                Arrays.asList(version("3.1.1M1"), version("4.0.0RC"), version("4.0.0")),
                versions.stream()
                        .filter(ArtifactVersionMatcher.build(Collections.emptyMap(), "gt(3.1.0)"))
                        .collect(Collectors.toList()));

        assertEquals(
                Arrays.asList(version("3.1.0"), version("3.1.1M1"), version("4.0.0RC"), version("4.0.0")),
                versions.stream()
                        .filter(ArtifactVersionMatcher.build(Collections.emptyMap(), "gte(3.1.0)"))
                        .collect(Collectors.toList()));

        assertEquals(
                Arrays.asList(version("2.0.0"), version("3.0.1"), version("3.0.2-alpha"), version("3.1.0M1")),
                versions.stream()
                        .filter(ArtifactVersionMatcher.build(Collections.emptyMap(), "lt(3.1.0)"))
                        .collect(Collectors.toList()));

        assertEquals(
                Arrays.asList(
                        version("2.0.0"),
                        version("3.0.1"),
                        version("3.0.2-alpha"),
                        version("3.1.0M1"),
                        version("3.1.0")),
                versions.stream()
                        .filter(ArtifactVersionMatcher.build(Collections.emptyMap(), "lte(3.1.0)"))
                        .collect(Collectors.toList()));

        assertEquals(
                Arrays.asList(version("2.0.0"), version("3.0.1"), version("3.1.0")),
                versions.stream()
                        .filter(ArtifactVersionMatcher.build(Collections.emptyMap(), "and(lte(3.1.0),noPreviews())"))
                        .collect(Collectors.toList()));

        assertEquals(
                Arrays.asList(version("2.0.0"), version("3.0.0")),
                Arrays.asList(
                                version("2.0.0-SNAPSHOT"),
                                version("2.0.0"),
                                version("3.0.0-20240524.224522-1"),
                                version("3.0.0"))
                        .stream()
                        .filter(ArtifactVersionMatcher.build(Collections.emptyMap(), "noSnapshots()"))
                        .collect(Collectors.toList()));
    }
}
