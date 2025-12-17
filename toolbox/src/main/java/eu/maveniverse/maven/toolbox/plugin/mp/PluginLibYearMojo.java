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
import eu.maveniverse.maven.toolbox.shared.ResolutionScope;
import eu.maveniverse.maven.toolbox.shared.Result;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
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
     * Artifact version matcher spec string to filter version candidates, default is 'any()'.
     */
    @Parameter(property = "artifactVersionMatcherSpec", defaultValue = "any()")
    private String artifactVersionMatcherSpec;

    /**
     * Artifact version selector spec string to select "latest", default is 'contextualSnapshotsAndPreviews()'.
     */
    @Parameter(property = "artifactVersionSelectorSpec", defaultValue = "contextualSnapshotsAndPreviews()")
    private String artifactVersionSelectorSpec;

    /**
     * Make libyear transitive, in which case it will calculate it for whole transitive hull.
     */
    @Parameter(property = "transitive", defaultValue = "false")
    private boolean transitive;

    /**
     * Make libyear show up-to-date libraries with age as well.
     */
    @Parameter(property = "upToDate", defaultValue = "false")
    private boolean upToDate;

    @Override
    protected Result<String> doExecute() throws Exception {
        ToolboxCommando toolboxCommando = getToolboxCommando();
        ArtifactMatcher artifactMatcher = toolboxCommando.parseArtifactMatcherSpec(artifactMatcherSpec);
        ArtifactVersionMatcher artifactVersionMatcher =
                toolboxCommando.parseArtifactVersionMatcherSpec(artifactVersionMatcherSpec);
        ArtifactVersionSelector artifactVersionSelector =
                toolboxCommando.parseArtifactVersionSelectorSpec(artifactVersionSelectorSpec);
        for (ResolutionRoot root : allProjectManagedPluginsAsResolutionRoots(toolboxCommando).stream()
                .filter(r -> artifactMatcher.test(r.getArtifact()))
                .toList()) {
            toolboxCommando.libYear(
                    "managed plugin " + root.getArtifact(),
                    ResolutionScope.RUNTIME,
                    root,
                    transitive,
                    upToDate,
                    artifactVersionMatcher,
                    artifactVersionSelector,
                    getRepositoryVendor());
        }
        for (ResolutionRoot root : allProjectPluginsAsResolutionRoots(toolboxCommando).stream()
                .filter(r -> artifactMatcher.test(r.getArtifact()))
                .toList()) {
            toolboxCommando.libYear(
                    "plugin " + root.getArtifact(),
                    ResolutionScope.RUNTIME,
                    root,
                    transitive,
                    upToDate,
                    artifactVersionMatcher,
                    artifactVersionSelector,
                    getRepositoryVendor());
        }
        return Result.success("Success");
    }
}
