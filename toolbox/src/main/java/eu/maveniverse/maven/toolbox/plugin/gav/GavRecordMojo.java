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
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.resolution.DependencyResolutionException;
import picocli.CommandLine;

/**
 * Controls recording of resolved Maven Artifacts.
 */
@CommandLine.Command(name = "record", description = "Controls recording of resolved Maven Artifacts")
@Mojo(name = "gav-record", requiresProject = false, threadSafe = true)
public final class GavRecordMojo extends GavMojoSupport {

    /**
     * Stops recording if set, otherwise starts it.
     */
    @CommandLine.Option(
            names = {"--start"},
            description = "Starts recording")
    @Parameter(property = "start", defaultValue = "false", required = true)
    private boolean start;

    /**
     * Stops recording if set, otherwise starts it.
     */
    @CommandLine.Option(
            names = {"--stop"},
            description = "Stops recording")
    @Parameter(property = "stop", defaultValue = "false", required = true)
    private boolean stop;

    @Override
    protected boolean doExecute(Output output, ToolboxCommando toolboxCommando) throws DependencyResolutionException {
        if (stop) {
            return toolboxCommando.recordStop(output);
        } else if (start) {
            return toolboxCommando.recordStart(output);
        } else {
            return toolboxCommando.recordStats(output);
        }
    }
}
