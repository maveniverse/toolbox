/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.cli;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.toolbox.shared.Artifacts;
import java.nio.file.Path;
import org.eclipse.aether.installation.InstallationException;
import picocli.CommandLine;

/**
 * Installs an artifact into local repository.
 */
@CommandLine.Command(name = "install", description = "Installs Maven Artifacts")
public final class Install extends ResolverCommandSupport {

    @CommandLine.Parameters(index = "0", description = "The GAV to install", arity = "1")
    private String gav;

    @CommandLine.Parameters(index = "1", description = "The artifact JAR file", arity = "1")
    private Path jar;

    @CommandLine.Option(
            names = {"--pom"},
            description = "The POM to install")
    private Path pom;

    @CommandLine.Option(
            names = {"--sources"},
            description = "The sources JAR to install")
    private Path sources;

    @CommandLine.Option(
            names = {"--javadoc"},
            description = "The javadoc JAR to install")
    private Path javadoc;

    @Override
    protected boolean doCall(Context context) throws InstallationException {
        Artifacts artifacts = new Artifacts(gav);
        artifacts.addMain(jar);
        if (pom != null) {
            artifacts.addPom(pom);
        }
        if (sources != null) {
            artifacts.addSources(sources);
        }
        if (javadoc != null) {
            artifacts.addJavadoc(javadoc);
        }
        return getToolboxCommando(context).install(artifacts, output);
    }
}
