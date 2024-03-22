/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.cli;

import eu.maveniverse.maven.toolbox.shared.ResolutionScope;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import picocli.CommandLine;

/**
 * Resolves transitively given artifact.
 */
@CommandLine.Command(name = "resolveTransitive", description = "Resolves Maven Artifacts Transitively")
public final class ResolveTransitive extends ResolverCommandSupport {

    @CommandLine.Parameters(index = "0..*", description = "The GAV to resolve", arity = "1")
    private java.util.List<String> gav;

    @CommandLine.Option(
            names = {"--resolutionScope"},
            defaultValue = "runtime",
            description = "Resolution scope to resolve (default main-runtime)")
    private String resolutionScope;

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
    protected boolean doCall(ToolboxCommando toolboxCommando) throws Exception {
        return toolboxCommando.resolveTransitive(
                ResolutionScope.parse(resolutionScope),
                toolboxCommando.loadGavs(gav, boms),
                sources,
                javadoc,
                signature,
                output);
    }
}
