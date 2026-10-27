/*
 * Copyright (c) 2023-2026 Maveniverse Org.
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

    /**
     * Experimental: also check and update plugins declared in POM profiles, writing updates back into the
     * profile they were declared in. When disabled (default), only the effective main build is considered.
     */
    @Parameter(property = "profiles", defaultValue = "false")
    private boolean profiles;

    @Override
    protected Result<Boolean> doExecute() throws Exception {
        ToolboxCommando toolboxCommando = getToolboxCommando();
        ArtifactMatcher artifactMatcher = toolboxCommando.parseArtifactMatcherSpec(artifactMatcherSpec);
        ArtifactVersionMatcher artifactVersionMatcher =
                toolboxCommando.parseArtifactVersionMatcherSpec(artifactVersionMatcherSpec);
        ArtifactVersionSelector artifactVersionSelector =
                toolboxCommando.parseArtifactVersionSelectorSpec(artifactVersionSelectorSpec);

        // Collect plugins per scope (null = main build, non-null = profile id)
        Map<String, List<ResolutionRoot>> managedPluginsPerScope =
                allProjectManagedPluginsAsResolutionRootsPerScope(toolboxCommando, profiles);
        Map<String, List<ResolutionRoot>> pluginsPerScope =
                allProjectPluginsAsResolutionRootsPerScope(toolboxCommando, profiles);

        Result<Map<Artifact, List<Version>>> managedPlugins = toolboxCommando.versions(
                "managed plugins",
                () -> distinctArtifacts(managedPluginsPerScope).stream().filter(artifactMatcher),
                artifactVersionMatcher,
                artifactVersionSelector);
        Result<Map<Artifact, List<Version>>> plugins = toolboxCommando.versions(
                "plugins",
                () -> distinctArtifacts(pluginsPerScope).stream().filter(artifactMatcher),
                artifactVersionMatcher,
                artifactVersionSelector);

        if (apply) {
            Map<String, List<Artifact>> managedPluginsUpdates = calculatePluginUpdatesPerScope(
                    toolboxCommando,
                    managedPlugins.getData().orElseThrow(),
                    managedPluginsPerScope,
                    artifactVersionSelector);
            Map<String, List<Artifact>> pluginsUpdates = calculatePluginUpdatesPerScope(
                    toolboxCommando, plugins.getData().orElseThrow(), pluginsPerScope, artifactVersionSelector);
            if (!managedPluginsUpdates.isEmpty() || !pluginsUpdates.isEmpty()) {
                try (ToolboxCommando.EditSession editSession =
                        toolboxCommando.createEditSession(mavenProject.getFile().toPath())) {
                    // Apply updates per scope so profile-declared plugins are updated in the right place.
                    applyPluginUpdatesPerScope(
                            toolboxCommando,
                            editSession,
                            managedPluginsUpdates,
                            ToolboxCommando.PomOpSubject.MANAGED_PLUGINS);
                    applyPluginUpdatesPerScope(
                            toolboxCommando, editSession, pluginsUpdates, ToolboxCommando.PomOpSubject.PLUGINS);
                }
            }
        }
        return Result.success(true);
    }
}
