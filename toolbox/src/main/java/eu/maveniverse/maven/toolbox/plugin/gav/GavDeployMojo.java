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
import eu.maveniverse.maven.toolbox.shared.ProjectArtifacts;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import java.io.File;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import picocli.CommandLine;

/**
 * Deploys Maven Artifact into remote repository.
 */
@CommandLine.Command(name = "deploy", description = "Deploys Maven Artifact into remote repository")
@Mojo(name = "gav-deploy", requiresProject = false, threadSafe = true)
public final class GavDeployMojo extends GavMojoSupport {

    /**
     * The remote repository spec (id::url).
     */
    @CommandLine.Parameters(index = "0", description = "The remote repository spec (id::url)", arity = "1")
    @Parameter(property = "remoteRepositorySpec", required = true)
    private String remoteRepositorySpec;

    /**
     * The GAV to deploy to.
     */
    @CommandLine.Parameters(index = "1", description = "The GAV to deploy to", arity = "1")
    @Parameter(property = "gav", required = true)
    private String gav;

    /**
     * The artifact JAR file.
     */
    @CommandLine.Parameters(index = "2", description = "The artifact JAR file", arity = "1")
    @Parameter(property = "jar", required = true)
    private File jar;

    /**
     * The POM file.
     */
    @CommandLine.Option(
            names = {"--pom"},
            description = "The POM file")
    @Parameter(property = "pom")
    private File pom;

    /**
     * The sources JAR file.
     */
    @CommandLine.Option(
            names = {"--sources"},
            description = "The sources JAR file")
    @Parameter(property = "sources")
    private File sources;

    /**
     * The javadoc JAR file.
     */
    @CommandLine.Option(
            names = {"--javadoc"},
            description = "The javadoc JAR file")
    @Parameter(property = "javadoc")
    private File javadoc;

    @Override
    protected boolean doExecute(Output output, ToolboxCommando toolboxCommando) throws Exception {
        ProjectArtifacts projectArtifacts = new ProjectArtifacts(gav);
        projectArtifacts.addMain(jar.toPath());
        if (pom != null) {
            projectArtifacts.addPom(pom.toPath());
        }
        if (sources != null) {
            projectArtifacts.addSources(sources.toPath());
        }
        if (javadoc != null) {
            projectArtifacts.addJavadoc(javadoc.toPath());
        }
        return toolboxCommando.deploy(
                toolboxCommando.parseRemoteRepository(remoteRepositorySpec), projectArtifacts, output);
    }
}
