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
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import java.nio.file.Path;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import picocli.CommandLine;

/**
 * Prints expected relative path for a given Maven Artifact in a local repository.
 */
@CommandLine.Command(
        name = "artifact-path",
        description = "Prints expected relative path for a given Maven Artifact in a local repository")
@Mojo(name = "gav-artifact-path", requiresProject = false, threadSafe = true)
public class GavArtifactPathMojo extends GavMojoSupport {
    /**
     * The GAV of artifact.
     */
    @CommandLine.Parameters(index = "0", description = "The Artifact coordinates", arity = "1")
    @Parameter(property = "gav", required = true)
    private String gav;

    /**
     * The optional remote repository spec string. It is expected to be in form of {@code id::url}, but we are really
     * interested in repository ID only.
     */
    @CommandLine.Option(
            names = {"--repository"},
            description =
                    "The optional remote repository spec string. It is expected to be in form of {@code id::url}, but we are really interested in repository ID only.")
    @Parameter(property = "repository")
    private String repository;

    @Override
    protected Result<Path> doExecute() throws Exception {
        ToolboxCommando toolboxCommando = getToolboxCommando();
        RemoteRepository rr = null;
        if (repository != null) {
            rr = toolboxCommando.parseRemoteRepository(repository);
        }
        return toolboxCommando.artifactPath(new DefaultArtifact(gav), rr);
    }
}
