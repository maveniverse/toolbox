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
import java.nio.file.Path;
import java.util.ArrayList;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.installation.InstallationException;
import org.eclipse.aether.util.artifact.SubArtifact;
import picocli.CommandLine;

/**
 * Installs an artifact into local repository.
 */
@CommandLine.Command(name = "install", description = "Installs Maven Artifacts")
public final class Install extends ResolverCommandSupport {

    @CommandLine.Parameters(index = "0", description = "The GAV to install")
    private String gav;

    @CommandLine.Parameters(index = "1", description = "The artifact JAR file")
    private Path jar;

    @CommandLine.Parameters(index = "2", description = "The artifact POM file")
    private Path pom;

    @Override
    protected Integer doCall(Context context) throws InstallationException {
        ArrayList<Artifact> artifacts = new ArrayList<>();
        artifacts.add(new DefaultArtifact(gav).setFile(jar.toFile()));
        artifacts.add(new SubArtifact(artifacts.get(0), "", "pom").setFile(pom.toFile()));
        return ToolboxCommando.getOrCreate(getContext()).install(artifacts, output) ? 0 : 1;
    }
}
