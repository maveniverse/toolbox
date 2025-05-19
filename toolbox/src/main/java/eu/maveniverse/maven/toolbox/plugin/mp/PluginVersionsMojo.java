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
import eu.maveniverse.maven.toolbox.shared.ArtifactVersionMatcher;
import eu.maveniverse.maven.toolbox.shared.ArtifactVersionSelector;
import eu.maveniverse.maven.toolbox.shared.ResolutionRoot;
import eu.maveniverse.maven.toolbox.shared.Result;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import java.util.List;
import java.util.Map;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.version.Version;
import picocli.CommandLine;

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
     * Artifact version selector spec string to select the version from candidates, default is 'last()'.
     */
    @CommandLine.Option(
            names = {"--artifactVersionSelectorSpec"},
            defaultValue = "last()",
            description = "Artifact version selector spec (default 'last()')")
    @Parameter(property = "artifactVersionSelectorSpec", defaultValue = "last()")
    private String artifactVersionSelectorSpec;

    /**
     * Apply results to POM.
     */
    @Parameter(property = "apply")
    private boolean apply;

    @Override
    protected Result<Boolean> doExecute() throws Exception {
        ToolboxCommando toolboxCommando = getToolboxCommando();
        ArtifactMatcher artifactMatcher = toolboxCommando.parseArtifactMatcherSpec(artifactMatcherSpec);
        ArtifactVersionMatcher artifactVersionMatcher =
                toolboxCommando.parseArtifactVersionMatcherSpec(artifactVersionMatcherSpec);
        ArtifactVersionSelector artifactVersionSelector =
                toolboxCommando.parseArtifactVersionSelectorSpec(artifactVersionSelectorSpec);

        Result<Map<Artifact, List<Version>>> managedPlugins = toolboxCommando.versions(
                "managed plugins",
                () -> allProjectManagedPluginsAsResolutionRoots(toolboxCommando).stream()
                        .map(ResolutionRoot::getArtifact)
                        .filter(artifactMatcher),
                artifactVersionMatcher,
                artifactVersionSelector);
        Result<Map<Artifact, List<Version>>> plugins = toolboxCommando.versions(
                "plugins",
                () -> allProjectPluginsAsResolutionRoots(toolboxCommando).stream()
                        .map(ResolutionRoot::getArtifact)
                        .filter(artifactMatcher),
                artifactVersionMatcher,
                artifactVersionSelector);

        if (apply) {
            List<Artifact> managedPluginsUpdates =
                    toolboxCommando.calculateUpdates(managedPlugins.getData().orElseThrow(), artifactVersionSelector);
            List<Artifact> pluginsUpdates =
                    toolboxCommando.calculateUpdates(plugins.getData().orElseThrow(), artifactVersionSelector);
            if (!managedPluginsUpdates.isEmpty() || !pluginsUpdates.isEmpty()) {
                try (ToolboxCommando.EditSession editSession =
                        toolboxCommando.createEditSession(mavenProject.getFile().toPath())) {
                    if (!managedPluginsUpdates.isEmpty()) {
                        toolboxCommando.editPom(
                                editSession,
                                ToolboxCommando.PomOpSubject.MANAGED_PLUGINS,
                                ToolboxCommando.Op.UPDATE,
                                managedPluginsUpdates::stream);
                    }
                    if (!pluginsUpdates.isEmpty()) {
                        toolboxCommando.editPom(
                                editSession,
                                ToolboxCommando.PomOpSubject.PLUGINS,
                                ToolboxCommando.Op.UPDATE,
                                pluginsUpdates::stream);
                    }
                }
            }
        }
        return Result.success(true);
    }
}
