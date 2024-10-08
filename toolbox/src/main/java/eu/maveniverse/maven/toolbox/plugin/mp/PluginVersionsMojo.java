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
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

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

    @Override
    protected Result<Boolean> doExecute() throws Exception {
        ToolboxCommando toolboxCommando = getToolboxCommando();
        ArtifactMatcher artifactMatcher = toolboxCommando.parseArtifactMatcherSpec(artifactMatcherSpec);
        toolboxCommando.versions(
                "managed plugins",
                () -> allProjectManagedPluginsAsResolutionRoots(toolboxCommando).stream()
                        .map(ResolutionRoot::getArtifact)
                        .filter(artifactMatcher),
                toolboxCommando.parseArtifactVersionMatcherSpec(artifactVersionMatcherSpec));
        toolboxCommando.versions(
                "plugins",
                () -> allProjectPluginsAsResolutionRoots(toolboxCommando).stream()
                        .map(ResolutionRoot::getArtifact)
                        .filter(artifactMatcher),
                toolboxCommando.parseArtifactVersionMatcherSpec(artifactVersionMatcherSpec));
        return Result.success(true);
    }
}
