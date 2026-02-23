/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.gav;

import eu.maveniverse.maven.toolbox.plugin.GavMojoSupport;
import eu.maveniverse.maven.toolbox.shared.Result;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import org.apache.maven.model.Model;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.artifact.DefaultArtifact;
import picocli.CommandLine;

/**
 * Flattens a BOM and outputs it to output.
 */
@CommandLine.Command(name = "flatten-bom", description = "Flattens a BOM")
@Mojo(name = "gav-flatten-bom", requiresProject = false, threadSafe = true)
public class GavFlattenBomMojo extends GavMojoSupport {
    /**
     * The GAV to emit flattened BOM with.
     */
    @CommandLine.Parameters(
            index = "0",
            defaultValue = "org.acme:acme:1.0",
            description = "The GAV to emit flattened BOM with")
    @Parameter(property = "gav", defaultValue = "org.acme:acme:1.0")
    private String gav;

    /**
     * The GAV of BOM to flatten.
     */
    @CommandLine.Parameters(index = "0", description = "The GAV of BOM to flatten")
    @Parameter(property = "bom", required = true)
    private String bom;

    /**
     * Whether to output verbose model or not.
     */
    @CommandLine.Option(
            names = {"--verbose"},
            description = "Whether to output verbose model or not.")
    @Parameter(property = "verbose", defaultValue = "false", required = true)
    private boolean verbose;

    @Override
    protected Result<Model> doExecute() throws Exception {
        ToolboxCommando toolboxCommando = getToolboxCommando();
        Result<Model> result = toolboxCommando.flattenBOM(new DefaultArtifact(gav), toolboxCommando.loadGav(bom));
        if (result.isSuccess()) {
            Model model = result.getData().orElseThrow();
            Result<String> modelString = toolboxCommando.modelToString(model, verbose);
            if (modelString.isSuccess()) {
                getOutput().tell(modelString.getData().orElseThrow());
            }
        }
        return result;
    }
}
