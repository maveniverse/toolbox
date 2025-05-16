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
import eu.maveniverse.maven.toolbox.shared.internal.jdom.JDomPomTransformer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.version.Version;
import picocli.CommandLine;

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
    @Parameter(property = "applyToPom")
    private boolean applyToPom;

    @Override
    protected Result<Boolean> doExecute() throws Exception {
        ToolboxCommando toolboxCommando = getToolboxCommando();
        ArtifactMatcher artifactMatcher = toolboxCommando.parseArtifactMatcherSpec(artifactMatcherSpec);
        ArtifactVersionMatcher artifactVersionMatcher =
                toolboxCommando.parseArtifactVersionMatcherSpec(artifactVersionMatcherSpec);
        ArtifactVersionSelector artifactVersionSelector =
                toolboxCommando.parseArtifactVersionSelectorSpec(artifactVersionSelectorSpec);

        Map<Artifact, List<Version>> allPlugins = new HashMap<>();
        for (MavenProject project : mavenSession.getProjects()) {
            Result<Map<Artifact, List<Version>>> managedPlugins = toolboxCommando.versions(
                    "managed plugins",
                    () -> allManagedPluginsAsResolutionRoots(toolboxCommando, project).stream()
                            .map(ResolutionRoot::getArtifact)
                            .filter(artifactMatcher),
                    artifactVersionMatcher,
                    artifactVersionSelector);
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
                    artifactVersionMatcher,
                    artifactVersionSelector);
            if (plugins.isSuccess()) {
                allPlugins.putAll(plugins.getData().orElseThrow());
            } else {
                return Result.failure(plugins.getMessage());
            }
        }

        if (applyToPom) {
            List<Artifact> pluginsUpdates = toolboxCommando.calculateLatest(allPlugins, artifactVersionSelector);
            if (!pluginsUpdates.isEmpty()) {
                try (ToolboxCommando.EditSession editSession =
                        toolboxCommando.createEditSession(mavenProject.getFile().toPath())) {
                    toolboxCommando.editPom(
                            editSession,
                            ToolboxCommando.PomOpSubject.MANAGED_PLUGINS,
                            ToolboxCommando.Op.UPSERT,
                            pluginsUpdates::stream);
                }
                for (MavenProject project : mavenSession.getProjects()) {
                    try (ToolboxCommando.EditSession editSession =
                            toolboxCommando.createEditSession(project.getFile().toPath())) {
                        toolboxCommando.editPom(
                                editSession,
                                pluginsUpdates.stream()
                                        .map(a -> JDomPomTransformer.deletePluginVersion()
                                                .apply(a))
                                        .collect(Collectors.toList()));
                    }
                }
            }
        }
        return Result.success(true);
    }
}
