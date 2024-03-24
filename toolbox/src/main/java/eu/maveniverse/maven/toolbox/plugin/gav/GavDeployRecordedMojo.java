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
import picocli.CommandLine;

/**
 * Deploys all recorded Maven Artifacts to remote repository.
 */
@CommandLine.Command(
        name = "deploy-recorded",
        description = "Deploys all recorded Maven Artifacts to remote repository")
@Mojo(name = "gav-deploy-recorded", requiresProject = false, threadSafe = true)
public final class GavDeployRecordedMojo extends GavMojoSupport {
    /**
     * The RemoteRepository spec (id::url).
     */
    @CommandLine.Parameters(index = "0", description = "The RemoteRepository spec (id::url)", arity = "1")
    @Parameter(property = "remoteRepositorySpec", required = true)
    private String remoteRepositorySpec;

    @Override
    protected boolean doExecute(Output output, ToolboxCommando toolboxCommando) throws Exception {
        return toolboxCommando.deployAllRecorded(remoteRepositorySpec, true, output);
    }
}
