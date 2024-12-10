/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.mp;

import eu.maveniverse.maven.toolbox.plugin.MPPluginMojoSupport;
import eu.maveniverse.maven.toolbox.shared.ArtifactMatcher;
import eu.maveniverse.maven.toolbox.shared.ResolutionRoot;
import eu.maveniverse.maven.toolbox.shared.Result;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import java.util.List;
import java.util.Map;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.version.Version;

/**
 * Lists available versions of Maven Project plugins.
 */
@Mojo(name = "plugin-versions", threadSafe = true)
public class PluginVersionsMojo extends MPPluginMojoSupport {
    /**
     * The plugin matcher spec.
     */
    @Parameter(property = "artifactMatcherSpec", defaultValue = "any()")
    private String artifactMatcherSpec;

    /**
     * Artifact version matcher spec string, default is 'noSnapshotsAndPreviews()'.
     */
    @Parameter(property = "artifactVersionMatcherSpec", defaultValue = "noSnapshotsAndPreviews()")
    private String artifactVersionMatcherSpec;

    /**
     * Apply results to POM.
     */
    @Parameter(property = "applyToPom")
    private boolean applyToPom;

    @Override
    protected Result<Boolean> doExecute() throws Exception {
        ToolboxCommando toolboxCommando = getToolboxCommando();
        ArtifactMatcher artifactMatcher = toolboxCommando.parseArtifactMatcherSpec(artifactMatcherSpec);
        Result<Map<Artifact, List<Version>>> managedPlugins = toolboxCommando.versions(
                "managed plugins",
                () -> allProjectManagedPluginsAsResolutionRoots(toolboxCommando).stream()
                        .map(ResolutionRoot::getArtifact)
                        .filter(artifactMatcher),
                toolboxCommando.parseArtifactVersionMatcherSpec(artifactVersionMatcherSpec));
        Result<Map<Artifact, List<Version>>> plugins = toolboxCommando.versions(
                "plugins",
                () -> allProjectPluginsAsResolutionRoots(toolboxCommando).stream()
                        .map(ResolutionRoot::getArtifact)
                        .filter(artifactMatcher),
                toolboxCommando.parseArtifactVersionMatcherSpec(artifactVersionMatcherSpec));

        if (applyToPom) {
            List<Artifact> managedPluginsUpdates =
                    toolboxCommando.calculateUpdates(managedPlugins.getData().orElseThrow());
            List<Artifact> pluginsUpdates =
                    toolboxCommando.calculateUpdates(plugins.getData().orElseThrow());
            if (!managedPluginsUpdates.isEmpty() || !pluginsUpdates.isEmpty()) {
                try (ToolboxCommando.EditSession editSession =
                        toolboxCommando.createEditSession(mavenProject.getFile().toPath())) {
                    if (!managedPluginsUpdates.isEmpty()) {
                        toolboxCommando.doManagedPlugins(
                                editSession, ToolboxCommando.Op.UPDATE, managedPluginsUpdates::stream);
                    }
                    if (!pluginsUpdates.isEmpty()) {
                        toolboxCommando.doPlugins(editSession, ToolboxCommando.Op.UPDATE, pluginsUpdates::stream);
                    }
                }
            }
        }
        return Result.success(true);
    }
}
