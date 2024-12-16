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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.version.Version;

/**
 * Locks available versions of Maven Project used plugins.
 */
@Mojo(name = "lock-plugin-versions", aggregator = true, threadSafe = true)
public class LockPluginVersionsMojo extends MPPluginMojoSupport {
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

        Map<Artifact, List<Version>> allPlugins = new HashMap<>();
        for (MavenProject project : mavenSession.getProjects()) {
            Result<Map<Artifact, List<Version>>> managedPlugins = toolboxCommando.versions(
                    "managed plugins",
                    () -> allManagedPluginsAsResolutionRoots(toolboxCommando, project).stream()
                            .map(ResolutionRoot::getArtifact)
                            .filter(artifactMatcher),
                    toolboxCommando.parseArtifactVersionMatcherSpec(artifactVersionMatcherSpec));
            if (managedPlugins.isSuccess()) {
                allPlugins.putAll(managedPlugins.getData().orElseThrow());
            } else {
                return Result.failure(managedPlugins.getMessage());
            }
            Result<Map<Artifact, List<Version>>> plugins = toolboxCommando.versions(
                    "plugins",
                    () -> allPluginsAsResolutionRoots(toolboxCommando, project).stream()
                            .map(ResolutionRoot::getArtifact)
                            .filter(artifactMatcher),
                    toolboxCommando.parseArtifactVersionMatcherSpec(artifactVersionMatcherSpec));
            if (plugins.isSuccess()) {
                allPlugins.putAll(plugins.getData().orElseThrow());
            } else {
                return Result.failure(plugins.getMessage());
            }
        }

        if (applyToPom) {
            List<Artifact> pluginsUpdates = toolboxCommando.calculateLatest(allPlugins);
            if (!pluginsUpdates.isEmpty()) {
                try (ToolboxCommando.EditSession editSession =
                        toolboxCommando.createEditSession(mavenProject.getFile().toPath())) {
                    toolboxCommando.doManagedPlugins(editSession, ToolboxCommando.Op.UPSERT, pluginsUpdates::stream);
                }
            }
        }
        return Result.success(true);
    }
}