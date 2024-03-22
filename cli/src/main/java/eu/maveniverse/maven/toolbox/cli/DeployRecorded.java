/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.cli;

import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import picocli.CommandLine;

/**
 * Deploys recorded artifacts to remote repository.
 */
@CommandLine.Command(name = "deployRecorded", description = "Deploys recorded Maven Artifacts")
public final class DeployRecorded extends ResolverCommandSupport {
    @CommandLine.Parameters(index = "0", description = "The RemoteRepository spec (id::url)")
    private String remoteRepositorySpec;

    @Override
    protected boolean doCall(ToolboxCommando toolboxCommando) throws Exception {
        return toolboxCommando.deployAllRecorded(remoteRepositorySpec, true, output);
    }
}
