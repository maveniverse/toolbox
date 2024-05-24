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
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import java.util.stream.Collectors;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.artifact.DefaultArtifact;
import picocli.CommandLine;

/**
 * Lists available versions Maven Artifacts.
 */
@CommandLine.Command(name = "versions", description = "Lists available versions for artifacts.")
@Mojo(name = "gav-versions", requiresProject = false, threadSafe = true)
public class GavVersionsMojo extends GavSearchMojoSupport {
    /**
     * The comma separated GAVs.
     */
    @CommandLine.Parameters(index = "0", description = "The comma separated GAVs", arity = "1")
    @Parameter(property = "gav", required = true)
    private String gav;

    /**
     * Allow to take into account snapshots.
     */
    @CommandLine.Option(
            names = {"--allowSnapshots"},
            description = "Allow to take into account snapshots")
    @Parameter(property = "allowSnapshots", defaultValue = "false")
    private boolean allowSnapshots;

    /**
     * Artifact version matcher spec string, default is 'noPreviews()'.
     */
    @CommandLine.Option(
            names = {"--artifactVersionMatcherSpec"},
            defaultValue = "noPreviews()",
            description = "Artifact version matcher spec (default 'noPreviews()')")
    @Parameter(property = "artifactVersionMatcherSpec", defaultValue = "noPreviews()")
    private String artifactVersionMatcherSpec;

    @Override
    protected boolean doExecute(Output output, ToolboxCommando toolboxCommando) throws Exception {
        return toolboxCommando.versions(
                slurp(gav).stream().map(DefaultArtifact::new).collect(Collectors.toList()),
                allowSnapshots,
                toolboxCommando.parseArtifactVersionMatcherSpec(artifactVersionMatcherSpec),
                output);
    }
}
