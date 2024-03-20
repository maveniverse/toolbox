/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.cli;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.toolbox.shared.ResolutionScope;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import picocli.CommandLine;

/**
 * Resolves transitively given artifact.
 */
@CommandLine.Command(name = "resolve", description = "Resolves Maven Artifacts")
public final class Resolve extends ResolverCommandSupport {

    @CommandLine.Parameters(index = "0", description = "The GAV to graph")
    private String gav;

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
            description = "Download sources JARs as well (best effort)")
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
        ToolboxCommando toolboxCommando = ToolboxCommando.getOrCreate(context);
        return toolboxCommando.resolve(
                ResolutionScope.parse(resolutionScope),
                toolboxCommando.toolboxResolver().loadGav(gav, boms),
                sources,
                javadoc,
                signature,
                output);
    }
}
