/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.gav;

import static eu.maveniverse.maven.toolbox.shared.input.StringSlurper.csv;

import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.toolbox.plugin.GavSearchMojoSupport;
import eu.maveniverse.maven.toolbox.shared.Result;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import java.io.IOException;
import java.util.Map;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.artifact.Artifact;
import picocli.CommandLine;

/**
 * Identify artifact, either by provided SHA-1 or calculated SHA-1 of a file pointed at.
 */
@CommandLine.Command(
        name = "identify",
        description = "Identify artifact, either by provided SHA-1 or calculated SHA-1 of a file pointed at")
@Mojo(name = "gav-identify", requiresProject = false, threadSafe = true)
public class GavIdentifyMojo extends GavSearchMojoSupport {
    /**
     * Target, SHA-1 checksum or a file (comma separated if more).
     */
    @CommandLine.Parameters(index = "0", description = "Target, a SHA-1 checksum or a file (comma separated if more)")
    @Parameter(property = "target", required = true)
    private String target;

    @CommandLine.Option(
            names = {"-d", "--decorated"},
            description = "Decorate the output")
    @Parameter(property = "decorated")
    private boolean decorated;

    @Override
    protected Result<Map<String, Artifact>> doExecute() throws IOException {
        ToolboxCommando toolboxCommando = getToolboxCommando();
        return toolboxCommando.identify(ContextOverrides.CENTRAL, csv(target), decorated);
    }
}
