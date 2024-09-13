/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.gav;

import eu.maveniverse.maven.toolbox.plugin.GavMojoSupport;
import eu.maveniverse.maven.toolbox.shared.ResolutionScope;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import picocli.CommandLine;

/**
 * Resolves Maven Artifact transitively and copies all of them to target.
 */
@CommandLine.Command(
        name = "copy-transitive",
        description = "Resolves Maven Artifact transitively and copies all of them to target")
@Mojo(name = "gav-copy-transitive", requiresProject = false, threadSafe = true)
public final class GavCopyTransitiveMojo extends GavMojoSupport {
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

    /**
     * The resolution scope to resolve (default is 'runtime').
     */
    @CommandLine.Option(
            names = {"--scope"},
            defaultValue = "runtime",
            description = "Resolution scope to resolve (default 'runtime')")
    @Parameter(property = "scope", defaultValue = "runtime", required = true)
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

    @Override
    protected boolean doExecute(Logger output, ToolboxCommando toolboxCommando) throws Exception {
        return toolboxCommando.copyTransitive(
                ResolutionScope.parse(scope),
                toolboxCommando.loadGavs(slurp(gav), slurp(boms)),
                toolboxCommando.artifactSink(sinkSpec),
                output);
    }
}
