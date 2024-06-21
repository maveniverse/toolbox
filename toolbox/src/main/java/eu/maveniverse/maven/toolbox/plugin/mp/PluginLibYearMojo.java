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
 * Calculates "libyear" for Maven Project (for plugins or transitively).
 */
@Mojo(name = "plugin-libyear", threadSafe = true)
public class PluginLibYearMojo extends MPPluginMojoSupport {
    /**
     * The plugin matcher spec.
     */
    @Parameter(property = "artifactMatcherSpec", defaultValue = "any()")
    private String artifactMatcherSpec;

    /**
     * Artifact version selector spec (default is 'noPreviews(major())').
     */
    @Parameter(property = "artifactVersionSelectorSpec", defaultValue = "noPreviews(major())")
    private String artifactVersionSelectorSpec;

    /**
     * Make libyear transitive, in which case it will calculate it for whole transitive hull.
     */
    @Parameter(property = "transitive", defaultValue = "false")
    private boolean transitive;

    /**
     * Make libyear quiet.
     */
    @Parameter(property = "quiet", defaultValue = "false")
    private boolean quiet;

    /**
     * Make libyear allow to take into account snapshots.
     */
    @Parameter(property = "allowSnapshots", defaultValue = "false")
    private boolean allowSnapshots;

    /**
     * Make libyear show up-to-date libraries with age as well.
     */
    @Parameter(property = "upToDate", defaultValue = "false")
    private boolean upToDate;

    @Override
    protected boolean doExecute(Output output, ToolboxCommando toolboxCommando) throws Exception {
        ArtifactMatcher artifactMatcher = toolboxCommando.parseArtifactMatcherSpec(artifactMatcherSpec);
        return toolboxCommando.libYear(
                ResolutionScope.RUNTIME,
                allPluginsAsResolutionRoots(toolboxCommando).stream()
                        .filter(r -> artifactMatcher.test(r.getArtifact()))
                        .collect(Collectors.toList()),
                transitive,
                quiet,
                allowSnapshots,
                upToDate,
                toolboxCommando.parseArtifactVersionSelectorSpec(artifactVersionSelectorSpec),
                getRepositoryVendor(),
                output);
    }
}
