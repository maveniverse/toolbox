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
import eu.maveniverse.maven.toolbox.shared.Output;
import eu.maveniverse.maven.toolbox.shared.ResolutionRoot;
import eu.maveniverse.maven.toolbox.shared.ResolutionScope;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
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
     * Artifact version matcher spec string to filter version candidates, default is 'noSnapshotsAndPreviews()'.
     */
    @Parameter(property = "artifactVersionMatcherSpec", defaultValue = "noSnapshotsAndPreviews()")
    private String artifactVersionMatcherSpec;

    /**
     * Artifact version selector spec string to select "latest", default is 'major()'.
     */
    @Parameter(property = "artifactVersionSelectorSpec", defaultValue = "major()")
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
     * Make libyear show up-to-date libraries with age as well.
     */
    @Parameter(property = "upToDate", defaultValue = "false")
    private boolean upToDate;

    @Override
    protected boolean doExecute(Output output, ToolboxCommando toolboxCommando) throws Exception {
        ArtifactMatcher artifactMatcher = toolboxCommando.parseArtifactMatcherSpec(artifactMatcherSpec);
        ArtifactVersionMatcher artifactVersionMatcher =
                toolboxCommando.parseArtifactVersionMatcherSpec(artifactVersionMatcherSpec);
        ArtifactVersionSelector artifactVersionSelector =
                toolboxCommando.parseArtifactVersionSelectorSpec(artifactVersionSelectorSpec);
        for (ResolutionRoot root : allProjectManagedPluginsAsResolutionRoots(toolboxCommando).stream()
                .filter(r -> artifactMatcher.test(r.getArtifact()))
                .collect(Collectors.toList())) {
            toolboxCommando.libYear(
                    "managed plugin " + root.getArtifact(),
                    ResolutionScope.RUNTIME,
                    root,
                    transitive,
                    quiet,
                    upToDate,
                    artifactVersionMatcher,
                    artifactVersionSelector,
                    getRepositoryVendor(),
                    output);
        }
        for (ResolutionRoot root : allProjectPluginsAsResolutionRoots(toolboxCommando).stream()
                .filter(r -> artifactMatcher.test(r.getArtifact()))
                .collect(Collectors.toList())) {
            toolboxCommando.libYear(
                    "plugin " + root.getArtifact(),
                    ResolutionScope.RUNTIME,
                    root,
                    transitive,
                    quiet,
                    upToDate,
                    artifactVersionMatcher,
                    artifactVersionSelector,
                    getRepositoryVendor(),
                    output);
        }
        return true;
    }
}
