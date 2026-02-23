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
import picocli.CommandLine;

/**
 * Shows effective model for given GAV.
 */
@CommandLine.Command(name = "effective-model", description = "Shows model of Maven Artifact")
@Mojo(name = "gav-effective-model", requiresProject = false, threadSafe = true)
public class GavEffectiveModelMojo extends GavMojoSupport {
    /**
     * The GAV to check for.
     */
    @CommandLine.Parameters(index = "0", description = "The GAV to check for")
    @Parameter(property = "gav", required = true)
    private String gav;

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
        Result<Model> result = toolboxCommando.effectiveModel(toolboxCommando.loadGav(gav));
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
