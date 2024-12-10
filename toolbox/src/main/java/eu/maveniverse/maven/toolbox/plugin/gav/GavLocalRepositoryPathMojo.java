/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.gav;

import eu.maveniverse.maven.toolbox.plugin.GavMojoSupport;
import eu.maveniverse.maven.toolbox.shared.Result;
import java.nio.file.Path;
import org.apache.maven.plugins.annotations.Mojo;
import picocli.CommandLine;

/**
 * Prints Maven Local Repository basedir.
 */
@CommandLine.Command(name = "local-repository-path", description = "Prints Maven Local Repository basedir")
@Mojo(name = "gav-local-repository-path", requiresProject = false, threadSafe = true)
public class GavLocalRepositoryPathMojo extends GavMojoSupport {
    @Override
    protected Result<Path> doExecute() throws Exception {
        return getToolboxCommando().localRepository();
    }
}
