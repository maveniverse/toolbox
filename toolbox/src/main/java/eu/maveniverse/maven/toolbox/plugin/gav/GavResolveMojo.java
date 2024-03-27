/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.gav;

import eu.maveniverse.maven.toolbox.plugin.GavMojoSupport;
import eu.maveniverse.maven.toolbox.shared.ArtifactSinks;
import eu.maveniverse.maven.toolbox.shared.Output;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import java.util.stream.Collectors;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
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

    @Override
    protected boolean doExecute(Output output, ToolboxCommando toolboxCommando) throws Exception {
        return toolboxCommando.resolve(
                csv(gav).stream().map(DefaultArtifact::new).collect(Collectors.toList()),
                sources,
                javadoc,
                signature,
                ArtifactSinks.nullArtifactSink(),
                output);
    }
}
