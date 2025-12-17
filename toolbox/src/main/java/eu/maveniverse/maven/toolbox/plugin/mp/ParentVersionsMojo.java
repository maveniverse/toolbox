/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.mp;

import static eu.maveniverse.maven.toolbox.shared.internal.domtrip.DOMTripUtils.toDomTrip;

import eu.maveniverse.maven.toolbox.plugin.MPPluginMojoSupport;
import eu.maveniverse.maven.toolbox.shared.ArtifactVersionMatcher;
import eu.maveniverse.maven.toolbox.shared.ArtifactVersionSelector;
import eu.maveniverse.maven.toolbox.shared.Result;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.version.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lists available parents of Maven Project.
 */
@Mojo(name = "parent-versions", threadSafe = true)
public class ParentVersionsMojo extends MPPluginMojoSupport {
    private static final Logger log = LoggerFactory.getLogger(ParentVersionsMojo.class);
    /**
     * Artifact version matcher spec string, default is 'any()'.
     */
    @Parameter(property = "artifactVersionMatcherSpec", defaultValue = "any()")
    private String artifactVersionMatcherSpec;

    /**
     * Artifact version selector spec string to select the version from candidates, default is 'contextualSnapshotsAndPreviews()'.
     */
    @Parameter(property = "artifactVersionSelectorSpec", defaultValue = "contextualSnapshotsAndPreviews()")
    private String artifactVersionSelectorSpec;

    /**
     * Apply results to POM.
     */
    @Parameter(property = "apply")
    private boolean apply;

    @Override
    protected Result<Boolean> doExecute() throws Exception {
        ToolboxCommando toolboxCommando = getToolboxCommando();
        ArtifactVersionMatcher artifactVersionMatcher =
                toolboxCommando.parseArtifactVersionMatcherSpec(artifactVersionMatcherSpec);
        ArtifactVersionSelector artifactVersionSelector =
                toolboxCommando.parseArtifactVersionSelectorSpec(artifactVersionSelectorSpec);

        if (!mavenSession.getRequest().isProjectPresent()
                || mavenProject.getModel().getParent() == null) {
            log.info("No parent found");
            return Result.success(Boolean.TRUE);
        }

        Artifact parentArtifact = new DefaultArtifact(
                mavenProject.getModel().getParent().getGroupId(),
                mavenProject.getModel().getParent().getArtifactId(),
                "pom",
                mavenProject.getModel().getParent().getVersion());

        Result<Map<Artifact, List<Version>>> parents = toolboxCommando.versions(
                "parent", () -> Stream.of(parentArtifact), artifactVersionMatcher, artifactVersionSelector);

        if (apply) {
            List<Artifact> parentsUpdates =
                    toolboxCommando.calculateUpdates(parents.getData().orElseThrow(), artifactVersionSelector);
            if (!parentsUpdates.isEmpty()) {
                try (ToolboxCommando.EditSession editSession =
                        toolboxCommando.createEditSession(mavenProject.getFile().toPath())) {
                    if (!parentsUpdates.isEmpty()) {
                        toolboxCommando.editPom(editSession, Collections.singletonList(s -> s.parent()
                                .updateParent(false, toDomTrip(parentsUpdates.get(0)))));
                    }
                }
            }
        }
        return Result.success(true);
    }
}
