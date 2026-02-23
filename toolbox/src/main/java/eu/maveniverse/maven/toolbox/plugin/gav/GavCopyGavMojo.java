/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.gav;

import static eu.maveniverse.maven.toolbox.shared.input.StringSlurper.slurp;

import eu.maveniverse.maven.toolbox.plugin.GavMojoSupport;
import eu.maveniverse.maven.toolbox.shared.Result;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import eu.maveniverse.maven.toolbox.shared.internal.ArtifactSources;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.artifact.Artifact;
import picocli.CommandLine;

/**
 * Resolves a given GAV and copies resulting artifact to target.
 *
 * @deprecated Use {@code copy resolve(gav(GAV)) sink} instead.
 */
@Deprecated
@CommandLine.Command(name = "copy-gav", description = "Resolves Maven Artifact and copies it to target")
@Mojo(name = "gav-copy-gav", requiresProject = false, threadSafe = true)
public final class GavCopyGavMojo extends GavMojoSupport {
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
    protected Result<List<Artifact>> doExecute() throws Exception {
        ToolboxCommando toolboxCommando = getToolboxCommando();
        return toolboxCommando.copy(
                ArtifactSources.concatArtifactSource(slurp(gav).stream()
                        .map(ArtifactSources::gavArtifactSource)
                        .collect(Collectors.toList())),
                toolboxCommando.artifactSink(sinkSpec, dryRun));
    }
}
