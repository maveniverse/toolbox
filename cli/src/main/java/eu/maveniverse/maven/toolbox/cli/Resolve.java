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
import java.util.stream.Collectors;
import org.eclipse.aether.artifact.DefaultArtifact;
import picocli.CommandLine;

/**
 * Resolves given artifact.
 */
@CommandLine.Command(name = "resolve", description = "Resolves Maven Artifacts")
public final class Resolve extends ResolverCommandSupport {

    @CommandLine.Parameters(index = "0..*", description = "The GAV to resolve", arity = "1")
    private java.util.List<String> gav;

    @CommandLine.Option(
            names = {"--boms"},
            defaultValue = "",
            split = ",",
            description = "Comma separated list of BOMs to apply")
    private java.util.List<String> boms;

    @CommandLine.Option(
            names = {"--sources"},
            description = "Download source JARs as well (best effort)")
    private boolean sources;

    @CommandLine.Option(
            names = {"--javadoc"},
            description = "Download javadoc JARs as well (best effort)")
    private boolean javadoc;

    @CommandLine.Option(
            names = {"--signature"},
            description = "Download signatures as well (best effort)")
    private boolean signature;

    @Override
    protected boolean doCall(Context context) throws Exception {
        ToolboxCommando toolboxCommando = getToolboxCommando(context);
        return toolboxCommando.resolve(
                gav.stream().map(DefaultArtifact::new).collect(Collectors.toList()),
                sources,
                javadoc,
                signature,
                output);
    }
}
