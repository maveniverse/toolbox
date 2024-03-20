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
import java.io.IOException;
import picocli.CommandLine;

/**
 * Checks given GAV for existence in a remote repository.
 */
@CommandLine.Command(name = "exists", description = "Checks Maven Artifact existence")
public final class Exists extends SearchCommandSupport {

    @CommandLine.Parameters(description = "The GAV to check")
    private String gav;

    @CommandLine.Option(
            names = {"--pom"},
            description = "Check POM presence as well (derive coordinates from GAV)")
    private boolean pom;

    @CommandLine.Option(
            names = {"--sources"},
            description = "Check sources JARs as well (derive coordinates from GAV)")
    private boolean sources;

    @CommandLine.Option(
            names = {"--javadoc"},
            description = "Check javadoc JARs as well (derive coordinates from GAV)")
    private boolean javadoc;

    @CommandLine.Option(
            names = {"--signature"},
            description = "Check PGP signature presence as well (derive coordinates from GAV)")
    private boolean signature;

    @CommandLine.Option(
            names = {"--all-required"},
            description =
                    "If set, any missing derived artifact will be reported as failure as well (otherwise just the specified GAVs presence is required)")
    private boolean allRequired;

    @Override
    protected boolean doCall(Context context) throws IOException {
        return ToolboxCommando.getOrCreate(context)
                .exists(getRemoteRepository(), gav, pom, sources, javadoc, signature, allRequired, output);
    }
}
