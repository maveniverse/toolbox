/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.hello;

import eu.maveniverse.maven.toolbox.plugin.GavMojoSupport;
import eu.maveniverse.maven.toolbox.shared.ArtifactVersionMatcher;
import eu.maveniverse.maven.toolbox.shared.ArtifactVersionSelector;
import java.util.List;
import java.util.stream.Stream;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import picocli.CommandLine;

/**
 * Support class for "hello" Mojos (not needing project).
 */
public abstract class HelloMojoSupport extends GavMojoSupport {
    /**
     * Artifact version matcher spec string to filter version candidates for parent, default is 'noSnapshotsAndPreviews()'.
     */
    @CommandLine.Option(
            names = {"--parentVersionMatcherSpec"},
            defaultValue = "noSnapshotsAndPreviews()",
            description = "Artifact version matcher spec for parent (default 'noSnapshotsAndPreviews()')")
    @Parameter(property = "parentVersionMatcherSpec", defaultValue = "noSnapshotsAndPreviews()")
    private String parentVersionMatcherSpec;

    /**
     * Artifact version selector spec string to select the version from candidates for parent, default is 'last()'.
     */
    @CommandLine.Option(
            names = {"--artifactVersionSelectorSpec"},
            defaultValue = "last()",
            description = "Artifact version selector spec (default 'last()')")
    @Parameter(property = "artifactVersionSelectorSpec", defaultValue = "last()")
    private String parentVersionSelectorSpec;

    /**
     * Accepts {@code G:A:V} or {@code G:A} in which case it appends ":1.0.0-SNAPSHOT".
     */
    protected Artifact toProjectArtifact(String gav) throws Exception {
        try {
            return new DefaultArtifact(gav);
        } catch (IllegalArgumentException ex) {
            return new DefaultArtifact(gav + ":1.0.0-SNAPSHOT");
        }
    }

    /**
     * Accepts {@code G:A:V} or {@code G:A} in which case tries to determine latest V of parent POM.
     */
    protected Artifact toParentArtifact(String gav) throws Exception {
        if (gav == null) {
            return null;
        }
        try {
            return new DefaultArtifact(gav);
        } catch (IllegalArgumentException e) {
            gav = gav + ":0";
            Artifact artifact = new DefaultArtifact(gav);
            ArtifactVersionMatcher artifactVersionMatcher =
                    getToolboxCommando().parseArtifactVersionMatcherSpec(parentVersionMatcherSpec);
            ArtifactVersionSelector artifactVersionSelector =
                    getToolboxCommando().parseArtifactVersionSelectorSpec(parentVersionSelectorSpec);
            List<Artifact> parentArtifacts = getToolboxCommando()
                    .calculateUpdates(
                            getToolboxCommando()
                                    .versions(
                                            "hello",
                                            () -> Stream.of(artifact),
                                            artifactVersionMatcher,
                                            artifactVersionSelector)
                                    .getData()
                                    .orElseThrow(),
                            artifactVersionSelector);
            return parentArtifacts.get(0);
        }
    }
}
