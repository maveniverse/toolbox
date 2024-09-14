/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.gav;

import eu.maveniverse.maven.toolbox.plugin.GavSearchMojoSupport;
import eu.maveniverse.maven.toolbox.shared.Output;
import eu.maveniverse.maven.toolbox.shared.ResolutionScope;
import eu.maveniverse.maven.toolbox.shared.Result;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import picocli.CommandLine;

/**
 * Calculates "libyear" for Maven Artifacts (for direct dependencies or transitively).
 */
@CommandLine.Command(name = "libyear", description = "Calculates libyear for artifacts.")
@Mojo(name = "gav-libyear", requiresProject = false, threadSafe = true)
public class GavLibYearMojo extends GavSearchMojoSupport {
    /**
     * The GAV to calculate libyear for.
     */
    @CommandLine.Parameters(index = "0", description = "The GAV to show libyear for", arity = "1")
    @Parameter(property = "gav", required = true)
    private String gav;

    /**
     * Resolution scope to resolve (default 'test').
     */
    @CommandLine.Option(
            names = {"--scope"},
            defaultValue = "test",
            description = "Resolution scope to resolve (default 'test')")
    @Parameter(property = "scope", defaultValue = "test", required = true)
    private String scope;

    /**
     * Comma separated list of BOMs to apply.
     */
    @CommandLine.Option(
            names = {"--boms"},
            defaultValue = "",
            description = "Comma separated list of BOMs to apply")
    @Parameter(property = "boms")
    private String boms;

    /**
     * Artifact version matcher spec string to filter version candidates, default is 'noSnapshotsAndPreviews()'.
     */
    @CommandLine.Option(
            names = {"--artifactVersionMatcherSpec"},
            defaultValue = "noSnapshotsAndPreviews()",
            description = "Artifact version matcher spec (default 'noSnapshotsAndPreviews()')")
    @Parameter(property = "artifactVersionMatcherSpec", defaultValue = "noSnapshotsAndPreviews()")
    private String artifactVersionMatcherSpec;

    /**
     * Artifact version selector spec string to select "latest", default is 'major()'.
     */
    @CommandLine.Option(
            names = {"--artifactVersionSelectorSpec"},
            defaultValue = "major()",
            description = "Artifact version selector spec (default 'major()')")
    @Parameter(property = "artifactVersionSelectorSpec", defaultValue = "major()")
    private String artifactVersionSelectorSpec;

    /**
     * Make libyear transitive, in which case it will calculate for whole transitive hull.
     */
    @CommandLine.Option(
            names = {"--transitive"},
            description = "Make command transitive")
    @Parameter(property = "transitive", defaultValue = "false")
    private boolean transitive;

    /**
     * Make libyear quiet.
     */
    @CommandLine.Option(
            names = {"--quiet"},
            description = "Make command quiet")
    @Parameter(property = "quiet", defaultValue = "false")
    private boolean quiet;

    /**
     * Make libyear show up-to-date libraries with age as well.
     */
    @CommandLine.Option(
            names = {"--upToDate"},
            description = "Make libyear show up-to-date libraries with age as well")
    @Parameter(property = "upToDate", defaultValue = "false")
    private boolean upToDate;

    @Override
    protected Result<Float> doExecute(Output output, ToolboxCommando toolboxCommando) throws Exception {
        return toolboxCommando.libYear(
                "GAV " + gav,
                ResolutionScope.parse(scope),
                toolboxCommando.loadGav(gav, slurp(boms)),
                transitive,
                quiet,
                upToDate,
                toolboxCommando.parseArtifactVersionMatcherSpec(artifactVersionMatcherSpec),
                toolboxCommando.parseArtifactVersionSelectorSpec(artifactVersionSelectorSpec),
                repositoryVendor,
                output);
    }
}
