/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.cli;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import java.util.HashSet;
import org.eclipse.aether.deployment.DeploymentException;
import picocli.CommandLine;

/**
 * Deploys recorded artifacts to remote repository.
 */
@CommandLine.Command(name = "deployRecorded", description = "Deploys recorded Maven Artifacts")
public final class DeployRecorded extends ResolverCommandSupport {
    @CommandLine.Parameters(index = "0", description = "The RemoteRepository spec (id::url)")
    private String remoteRepositorySpec;

    @Override
    protected Integer doCall(Context context) throws DeploymentException {
        normal("Deploying recorded");

        ToolboxCommando toolboxCommando = ToolboxCommando.getOrCreate(getContext());
        toolboxCommando.artifactRecorder().setActive(false);
        return toolboxCommando.deploy(
                        remoteRepositorySpec,
                        new HashSet<>(toolboxCommando.artifactRecorder().getAllArtifacts()),
                        output)
                ? 0
                : 1;
    }
}
