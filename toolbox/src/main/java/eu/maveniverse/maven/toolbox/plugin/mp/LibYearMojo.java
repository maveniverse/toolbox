/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.mp;

import eu.maveniverse.maven.toolbox.plugin.MPMojoSupport;
import eu.maveniverse.maven.toolbox.shared.Output;
import eu.maveniverse.maven.toolbox.shared.ResolutionScope;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

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
     * The dependency matcher spec.
     */
    @Parameter(property = "depSpec", defaultValue = "any()")
    private String depSpec;

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

    @Override
    protected boolean doExecute(Output output, ToolboxCommando toolboxCommando) throws Exception {
        ResolutionScope resolutionScope = ResolutionScope.parse(scope);
        return toolboxCommando.libYear(
                resolutionScope,
                projectDependenciesAsResolutionRoots(
                        resolutionScope, toolboxCommando.parseDependencyMatcherSpec(depSpec)),
                transitive,
                quiet,
                allowSnapshots,
                toolboxCommando.parseArtifactVersionSelectorSpec(artifactVersionSelectorSpec),
                output);
    }
}
