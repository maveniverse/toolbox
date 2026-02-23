/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.gav;

import eu.maveniverse.maven.toolbox.plugin.GavSearchMojoSupport;
import eu.maveniverse.maven.toolbox.shared.Result;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import java.io.IOException;
import java.util.Map;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.artifact.Artifact;
import picocli.CommandLine;

/**
 * Checks given GAV for existence in a remote repository.
 */
@CommandLine.Command(name = "exists", description = "Checks Maven Artifact existence")
@Mojo(name = "gav-exists", requiresProject = false, threadSafe = true)
public class GavExistsMojo extends GavSearchMojoSupport {
    /**
     * The GAV to check for.
     */
    @CommandLine.Parameters(index = "0", description = "The GAV to check for")
    @Parameter(property = "gav", required = true)
    private String gav;

    /**
     * Check POM presence as well (derive coordinates from GAV).
     */
    @CommandLine.Option(
            names = {"--pom"},
            description = "Check POM presence as well (derive coordinates from GAV)")
    @Parameter(property = "pom", defaultValue = "false")
    private boolean pom;

    /**
     * Check sources JAR as well (derive coordinates from GAV).
     */
    @CommandLine.Option(
            names = {"--sources"},
            description = "Check sources JAR as well (derive coordinates from GAV)")
    @Parameter(property = "sources", defaultValue = "false")
    private boolean sources;

    /**
     * Check javadoc JAR as well (derive coordinates from GAV).
     */
    @CommandLine.Option(
            names = {"--javadoc"},
            description = "Check javadoc JAR as well (derive coordinates from GAV)")
    @Parameter(property = "javadoc", defaultValue = "false")
    private boolean javadoc;

    /**
     * Check GnuPG signature as well (derive coordinates from GAV).
     */
    @CommandLine.Option(
            names = {"--signature"},
            description = "Check GnuPG signature as well (derive coordinates from GAV)")
    @Parameter(property = "signature", defaultValue = "false")
    private boolean signature;

    /**
     * If set, any missing derived artifact will be reported as failure as well. Otherwise, just the specified GAVs
     * presence is required.
     */
    @CommandLine.Option(
            names = {"--all-required"},
            description =
                    "If set, any missing derived artifact will be reported as failure as well. Otherwise just the specified GAVs presence is required")
    @Parameter(property = "allRequired", defaultValue = "false")
    private boolean allRequired;

    @Override
    protected Result<Map<Artifact, Boolean>> doExecute() throws IOException {
        ToolboxCommando toolboxCommando = getToolboxCommando();
        return toolboxCommando.exists(
                getRemoteRepository(toolboxCommando), gav, pom, sources, javadoc, signature, allRequired, null);
    }
}
