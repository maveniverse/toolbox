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
 * Collects given GAV and output its dependency tree.
 */
@CommandLine.Command(name = "tree", description = "Displays dependency tree")
public final class Tree extends ResolverCommandSupport {

    @CommandLine.Parameters(index = "0", description = "The GAV to graph")
    private String gav;

    @CommandLine.Option(
            names = {"--resolutionScope"},
            defaultValue = "runtime",
            description = "Resolution scope to resolve (default main-runtime)")
    private String resolutionScope;

    @CommandLine.Option(
            names = {"--verboseTree"},
            defaultValue = "false",
            description = "Whether the displayed tree needs to be verbose or not (default false)")
    private boolean verboseTree;

    @CommandLine.Option(
            names = {"--boms"},
            defaultValue = "",
            split = ",",
            description = "Comma separated list of BOMs to apply")
    private java.util.List<String> boms;

    @Override
    protected boolean doCall(Context context) throws Exception {
        ToolboxCommando toolboxCommando = ToolboxCommando.getOrCreate(context);
        return toolboxCommando.tree(
                ResolutionScope.parse(resolutionScope),
                toolboxCommando.toolboxResolver().loadGav(gav, boms),
                verboseTree,
                output);
    }
}
