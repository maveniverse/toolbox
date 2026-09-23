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
import java.util.ArrayList;
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
                allProjectManagedPluginsAsResolutionRootsPerScope(toolboxCommando);
        Map<String, List<ResolutionRoot>> pluginsPerScope = allProjectPluginsAsResolutionRootsPerScope(toolboxCommando);

        // Flatten all roots for version resolution (scope is not relevant for lookup, only for write-back)
        List<ResolutionRoot> allManagedPlugins = new ArrayList<>();
        managedPluginsPerScope.values().forEach(allManagedPlugins::addAll);
        List<ResolutionRoot> allPlugins = new ArrayList<>();
        pluginsPerScope.values().forEach(allPlugins::addAll);

        Result<Map<Artifact, List<Version>>> managedPlugins = toolboxCommando.versions(
                "managed plugins",
                () -> allManagedPlugins.stream()
                        .map(ResolutionRoot::getArtifact)
                        .filter(artifactMatcher),
                artifactVersionMatcher,
                artifactVersionSelector);
        Result<Map<Artifact, List<Version>>> plugins = toolboxCommando.versions(
                "plugins",
                () -> allPlugins.stream().map(ResolutionRoot::getArtifact).filter(artifactMatcher),
                artifactVersionMatcher,
                artifactVersionSelector);

        if (apply) {
            if (!managedPlugins.getData().orElseThrow().isEmpty()
                    || !plugins.getData().orElseThrow().isEmpty()) {
                try (ToolboxCommando.EditSession editSession =
                        toolboxCommando.createEditSession(mavenProject.getFile().toPath())) {
                    // Apply updates per scope so profile-declared plugins are updated in the right place.
                    // Updates are calculated per scope to avoid a same-GA plugin declared in multiple scopes
                    // with different versions causing one scope's target version to bleed into another.
                    applyPluginUpdatesPerScope(
                            toolboxCommando,
                            editSession,
                            managedPlugins.getData().orElseThrow(),
                            managedPluginsPerScope,
                            artifactVersionSelector,
                            ToolboxCommando.PomOpSubject.MANAGED_PLUGINS);
                    applyPluginUpdatesPerScope(
                            toolboxCommando,
                            editSession,
                            plugins.getData().orElseThrow(),
                            pluginsPerScope,
                            artifactVersionSelector,
                            ToolboxCommando.PomOpSubject.PLUGINS);
                }
            }
        }
        return Result.success(true);
    }
}
