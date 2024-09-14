/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.gav;

import eu.maveniverse.maven.toolbox.plugin.GavSearchMojoSupport;
import eu.maveniverse.maven.toolbox.shared.Output;
import eu.maveniverse.maven.toolbox.shared.Result;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import java.io.IOException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import picocli.CommandLine;

/**
 * Verifies Maven Artifact against known SHA-1 using origin repository.
 */
@CommandLine.Command(
        name = "verify",
        description = "Verifies Maven Artifact against known SHA-1 using origin repository")
@Mojo(name = "gav-verify", requiresProject = false, threadSafe = true)
public class GavVerifyMojo extends GavSearchMojoSupport {
    /**
     * The GAV to verify.
     */
    @CommandLine.Parameters(index = "0", description = "The GAV to show tree for", arity = "1")
    @Parameter(property = "gav", required = true)
    private String gav;

    /**
     * The locally known SHA-1 of GAV.
     */
    @CommandLine.Parameters(index = "1", description = "The locally known SHA-1 of GAV")
    @Parameter(property = "sha1", required = true)
    private String sha1;

    @Override
    protected Result<Boolean> doExecute(Output output, ToolboxCommando toolboxCommando) throws IOException {
        return toolboxCommando.verify(getRemoteRepository(toolboxCommando), gav, sha1, null, output);
    }
}
