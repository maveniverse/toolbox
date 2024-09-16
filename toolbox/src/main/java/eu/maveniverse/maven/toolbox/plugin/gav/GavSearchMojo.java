/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.gav;

import eu.maveniverse.maven.toolbox.plugin.GavSearchMojoSupport;
import eu.maveniverse.maven.toolbox.shared.Result;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import eu.maveniverse.maven.toolbox.shared.output.Output;
import java.io.IOException;
import java.util.List;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.artifact.Artifact;
import picocli.CommandLine;

/**
 * Searches Maven Artifacts using SMO service.
 */
@CommandLine.Command(name = "search", description = "Searches Maven Artifacts using SMO service")
@Mojo(name = "gav-search", requiresProject = false, threadSafe = true)
public class GavSearchMojo extends GavSearchMojoSupport {
    /**
     * The expression to search for.
     */
    @CommandLine.Parameters(index = "0", description = "The expression to search for")
    @Parameter(property = "expression", required = true)
    private String expression;

    @Override
    protected Result<List<Artifact>> doExecute(Output output, ToolboxCommando toolboxCommando) throws IOException {
        return toolboxCommando.search(getRemoteRepository(toolboxCommando), expression, output);
    }
}
