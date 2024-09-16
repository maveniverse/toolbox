/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.gav;

import eu.maveniverse.maven.toolbox.plugin.GavMojoSupport;
import eu.maveniverse.maven.toolbox.shared.Result;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import eu.maveniverse.maven.toolbox.shared.output.Output;
import java.util.List;
import java.util.Map;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.version.Version;
import picocli.CommandLine;

/**
 * Lists available versions Maven Artifacts.
 */
@CommandLine.Command(name = "versions", description = "Lists available versions for artifacts.")
@Mojo(name = "gav-versions", requiresProject = false, threadSafe = true)
public class GavVersionsMojo extends GavMojoSupport {
    /**
     * The comma separated GAVs.
     */
    @CommandLine.Parameters(index = "0", description = "The comma separated GAVs", arity = "1")
    @Parameter(property = "gav", required = true)
    private String gav;

    /**
     * Artifact version matcher spec string to filter version candidates, default is 'noSnapshotsAndPreviews()'.
     */
    @CommandLine.Option(
            names = {"--artifactVersionMatcherSpec"},
            defaultValue = "noSnapshotsAndPreviews()",
            description = "Artifact version matcher spec (default 'noSnapshotsAndPreviews()')")
    @Parameter(property = "artifactVersionMatcherSpec", defaultValue = "noSnapshotsAndPreviews()")
    private String artifactVersionMatcherSpec;

    @Override
    protected Result<Map<Artifact, List<Version>>> doExecute(Output output, ToolboxCommando toolboxCommando)
            throws Exception {
        return toolboxCommando.versions(
                "GAV",
                () -> slurp(gav).stream().map(DefaultArtifact::new),
                toolboxCommando.parseArtifactVersionMatcherSpec(artifactVersionMatcherSpec),
                output);
    }
}
