/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.gav;

import eu.maveniverse.maven.toolbox.plugin.GavMojoSupport;
import eu.maveniverse.maven.toolbox.shared.Output;
import eu.maveniverse.maven.toolbox.shared.ResolutionScope;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import picocli.CommandLine;

/**
 * Booms
 */
@CommandLine.Command(name = "boom", description = "Boom")
@Mojo(name = "boom", requiresProject = false, threadSafe = true)
public class GavBoomMojo extends GavMojoSupport {
    @Override
    protected boolean doExecute(Output output, ToolboxCommando toolboxCommando) throws Exception {
        MojoExecutionException executionException = new MojoExecutionException("boom", new RuntimeException("cause"));
        executionException.addSuppressed(new RuntimeException("suppressed"));
        throw executionException;
    }
}
