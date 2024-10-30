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
import java.util.List;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import picocli.CommandLine;

/**
 * Resolves Maven Artifacts.
 */
@CommandLine.Command(name = "resolve", description = "Resolves Maven Artifacts")
@Mojo(name = "gav-resolve", requiresProject = false, threadSafe = true)
public class GavResolveMojo extends GavMojoSupport {
    /**
     * The comma separated GAVs to resolve.
     */
    @CommandLine.Parameters(index = "0", description = "The comma separated GAVs to resolve", arity = "1")
    @Parameter(property = "gav", required = true)
    private String gav;

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
     * Resolve sources JAR as well (derive coordinates from GAV).
     */
    @CommandLine.Option(
            names = {"--sources"},
            description = "Resolve sources JAR as well (derive coordinates from GAV)")
    @Parameter(property = "sources", defaultValue = "false")
    private boolean sources;

    /**
     * Resolve javadoc JAR as well (derive coordinates from GAV).
     */
    @CommandLine.Option(
            names = {"--javadoc"},
            description = "Resolve javadoc JAR as well (derive coordinates from GAV)")
    @Parameter(property = "javadoc", defaultValue = "false")
    private boolean javadoc;

    /**
     * Resolve GnuPG signature as well (derive coordinates from GAV).
     */
    @CommandLine.Option(
            names = {"--signature"},
            description = "Resolve GnuPG signature as well (derive coordinates from GAV)")
    @Parameter(property = "signature", defaultValue = "false")
    private boolean signature;

    /**
     * The artifact sink spec (default: "null()").
     */
    @CommandLine.Option(
            names = {"--sinkSpec"},
            defaultValue = "null()",
            description = "The sink spec (default 'null()')")
    @Parameter(property = "sinkSpec", defaultValue = "null()", required = true)
    private String sinkSpec;

    @Override
    protected Result<List<Artifact>> doExecute() throws Exception {
        ToolboxCommando toolboxCommando = getToolboxCommando();
        return toolboxCommando.resolve(
                () -> slurp(gav).stream().map(DefaultArtifact::new),
                sources,
                javadoc,
                signature,
                toolboxCommando.artifactSink(sinkSpec, dryRun));
    }
}
