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
import java.io.File;
import java.util.List;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.artifact.Artifact;
import picocli.CommandLine;

/**
 * Installs an artifact into local repository.
 */
@CommandLine.Command(name = "install", description = "Installs an artifact into local repository")
@Mojo(name = "gav-install", requiresProject = false, threadSafe = true)
public final class GavInstallMojo extends GavMojoSupport {

    /**
     * The GAV to deploy to.
     */
    @CommandLine.Parameters(index = "0", description = "The GAV to install to", arity = "1")
    @Parameter(property = "gav", required = true)
    private String gav;

    /**
     * The artifact JAR file.
     */
    @CommandLine.Parameters(index = "1", description = "The artifact JAR file", arity = "1")
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
    protected Result<List<Artifact>> doExecute() throws Exception {
        ToolboxCommando toolboxCommando = getToolboxCommando();
        return toolboxCommando.copy(
                projectArtifacts(gav, jar, pom, sources, javadoc), toolboxCommando.artifactSink("install()"));
    }
}
