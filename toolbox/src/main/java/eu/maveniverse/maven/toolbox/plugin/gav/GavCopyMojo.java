/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.gav;

import eu.maveniverse.maven.toolbox.plugin.GavMojoSupport;
import eu.maveniverse.maven.toolbox.shared.Output;
import eu.maveniverse.maven.toolbox.shared.Result;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import java.util.List;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import picocli.CommandLine;

/**
 * Resolves a given GAV and copies resulting artifact to target.
 */
@CommandLine.Command(name = "copy", description = "Resolves Maven Artifact and copies it to target")
@Mojo(name = "gav-copy", requiresProject = false, threadSafe = true)
public final class GavCopyMojo extends GavMojoSupport {
    /**
     * The sink spec.
     */
    @CommandLine.Parameters(index = "0", description = "The sink spec", arity = "1")
    @Parameter(property = "sinkSpec", required = true)
    private String sinkSpec;

    /**
     * The comma separated GAVs to resolve.
     */
    @CommandLine.Parameters(index = "1", description = "The comma separated GAVs to resolve", arity = "1")
    @Parameter(property = "gav", required = true)
    private String gav;

    @Override
    protected Result<List<Artifact>> doExecute(Output output, ToolboxCommando toolboxCommando) throws Exception {
        return toolboxCommando.copy(
                () -> slurp(gav).stream().map(DefaultArtifact::new), toolboxCommando.artifactSink(sinkSpec), output);
    }
}
