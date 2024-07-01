/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.mp;

import eu.maveniverse.maven.toolbox.plugin.MPPluginMojoSupport;
import eu.maveniverse.maven.toolbox.shared.*;
import java.util.stream.Collectors;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Lists available versions of Maven Project plugins.
 */
@Mojo(name = "plugin-versions", threadSafe = true)
public class PluginVersionsMojo extends MPPluginMojoSupport {
    /**
     * Allow to take into account snapshots.
     */
    @Parameter(property = "allowSnapshots", defaultValue = "false")
    private boolean allowSnapshots;

    /**
     * The plugin matcher spec.
     */
    @Parameter(property = "artifactMatcherSpec", defaultValue = "any()")
    private String artifactMatcherSpec;

    /**
     * Artifact version matcher spec string, default is 'not(preview())'.
     */
    @Parameter(property = "artifactVersionMatcherSpec", defaultValue = "not(preview())")
    private String artifactVersionMatcherSpec;

    @Override
    protected boolean doExecute(Output output, ToolboxCommando toolboxCommando) throws Exception {
        ArtifactMatcher artifactMatcher = toolboxCommando.parseArtifactMatcherSpec(artifactMatcherSpec);
        return toolboxCommando.versions(
                allPluginsAsResolutionRoots(toolboxCommando).stream()
                        .map(ResolutionRoot::getArtifact)
                        .filter(artifactMatcher)
                        .collect(Collectors.toList()),
                allowSnapshots,
                toolboxCommando.parseArtifactVersionMatcherSpec(artifactVersionMatcherSpec),
                output);
    }
}
