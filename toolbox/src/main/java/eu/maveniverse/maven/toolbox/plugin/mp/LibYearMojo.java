/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.mp;

import eu.maveniverse.maven.toolbox.plugin.MPMojoSupport;
import eu.maveniverse.maven.toolbox.shared.ResolutionScope;
import eu.maveniverse.maven.toolbox.shared.Result;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;

/**
 * Calculates "libyear" for Maven Projects (for direct dependencies or transitively).
 */
@Mojo(name = "libyear", threadSafe = true)
public class LibYearMojo extends MPMojoSupport {
    /**
     * Resolution scope to resolve (default 'test').
     */
    @Parameter(property = "scope", defaultValue = "test", required = true)
    private String scope;

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
    protected Result<Float> doExecute(Logger output, ToolboxCommando toolboxCommando) throws Exception {
        return toolboxCommando.libYear(
                "project " + mavenProject.getId(),
                ResolutionScope.parse(scope),
                projectAsResolutionRoot(),
                transitive,
                quiet,
                upToDate,
                toolboxCommando.parseArtifactVersionMatcherSpec(artifactVersionMatcherSpec),
                toolboxCommando.parseArtifactVersionSelectorSpec(artifactVersionSelectorSpec),
                getRepositoryVendor(),
                output);
    }
}
