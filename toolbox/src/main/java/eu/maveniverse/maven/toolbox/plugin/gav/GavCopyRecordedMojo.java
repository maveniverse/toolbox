/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.gav;

import eu.maveniverse.maven.toolbox.plugin.GavMojoSupport;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import picocli.CommandLine;

/**
 * Copy all recorded Maven Artifacts to given sink.
 */
@CommandLine.Command(name = "copy-recorded", description = "Copies all recorded Maven Artifacts to specified sink")
@Mojo(name = "gav-copy-recorded", requiresProject = false, threadSafe = true)
public final class GavCopyRecordedMojo extends GavMojoSupport {
    /**
     * The sink spec.
     */
    @CommandLine.Parameters(index = "0", description = "The sink spec", arity = "1")
    @Parameter(property = "sinkSpec", required = true)
    private String sinkSpec;

    @Override
    protected boolean doExecute(Logger output, ToolboxCommando toolboxCommando) throws Exception {
        return toolboxCommando.copyAllRecorded(toolboxCommando.artifactSink(sinkSpec), true, output);
    }
}
