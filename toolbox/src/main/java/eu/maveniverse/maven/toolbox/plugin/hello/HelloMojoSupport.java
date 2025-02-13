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
import eu.maveniverse.maven.toolbox.shared.Result;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.version.Version;
import picocli.CommandLine;

/**
 * Support class for "hello" Mojos (not needing project).
 */
public abstract class HelloMojoSupport extends GavMojoSupport {
    protected final Path rootPom;

    public HelloMojoSupport() {
        this.rootPom = Path.of("pom.xml").toAbsolutePath();
    }

    /**
     * Artifact version matcher spec string to filter version candidates for parent, default is 'noSnapshotsAndPreviews()'.
     */
    @CommandLine.Option(
            names = {"--parentVersionMatcherSpec"},
            defaultValue = "noSnapshotsAndPreviews()",
            description = "Artifact version matcher spec for parent (default 'noSnapshotsAndPreviews()')")
    @Parameter(property = "parentVersionMatcherSpec", defaultValue = "noSnapshotsAndPreviews()")
    protected String parentVersionMatcherSpec;

    /**
     * Artifact version selector spec string to select the version from candidates for parent, default is 'last()'.
     */
    @CommandLine.Option(
            names = {"--artifactVersionSelectorSpec"},
            defaultValue = "last()",
            description = "Artifact version selector spec (default 'last()')")
    @Parameter(property = "artifactVersionSelectorSpec", defaultValue = "last()")
    protected String parentVersionSelectorSpec;

    /**
     * Force overwrite of existing POM file.
     */
    @CommandLine.Option(
            names = {"--force"},
            description = "Force overwrite of existing POM file")
    @Parameter(property = "force")
    protected boolean force;

    /**
     * Accepts {@code G:A:V} or {@code G:A} in which case it appends ":1.0.0-SNAPSHOT".
     */
    protected Artifact toRootProjectArtifact(String gav) throws Exception {
        try {
            return new DefaultArtifact(gav);
        } catch (IllegalArgumentException ex) {
            return new DefaultArtifact(gav + ":1.0.0-SNAPSHOT");
        }
    }

    /**
     * Accepts {@code G:A:V} or {@code G:A} in which case tries to determine latest V of parent POM.
     */
    private Artifact toLatestArtifact(String gav) throws Exception {
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
            Result<Map<Artifact, List<Version>>> versions = getToolboxCommando()
                    .versions("hello", () -> Stream.of(artifact), artifactVersionMatcher, artifactVersionSelector);
            if (versions.isSuccess()) {
                List<Artifact> parentArtifacts = getToolboxCommando()
                        .calculateUpdates(versions.getData().orElseThrow(), artifactVersionSelector);
                if (parentArtifacts.isEmpty()) {
                    throw new IllegalStateException("No parent artifacts found for " + gav);
                }
                return parentArtifacts.get(0);
            } else {
                throw new IllegalStateException("Could not select latest version of parent " + artifact);
            }
        }
    }

    /**
     * Accepts {@code G:A:V} or {@code G:A} in which case tries to determine latest V of parent POM.
     */
    protected Artifact toParentArtifact(String gav) throws Exception {
        return toLatestArtifact(gav);
    }

    /**
     * Accepts {@code G:A:V} or {@code G:A} in which case tries to determine latest V of parent POM.
     */
    protected Artifact toDependencyArtifact(String gav) throws Exception {
        return toLatestArtifact(gav);
    }

    /**
     * Accepts {@code G:A:V} or {@code G:A} in which case tries to determine latest V of parent POM.
     */
    protected Artifact toPluginArtifact(String gav) throws Exception {
        return toLatestArtifact(gav);
    }
}
