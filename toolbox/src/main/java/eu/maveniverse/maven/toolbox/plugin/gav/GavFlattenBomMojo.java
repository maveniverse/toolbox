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
import org.apache.maven.model.Model;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.artifact.DefaultArtifact;
import picocli.CommandLine;

/**
 * Flattens a BOM.
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

    @Override
    protected Result<Model> doExecute() throws Exception {
        ToolboxCommando toolboxCommando = getToolboxCommando();
        return toolboxCommando.flattenBOM(new DefaultArtifact(gav), toolboxCommando.loadGav(bom));
    }
}
