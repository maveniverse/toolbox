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
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.collection.CollectResult;
import org.slf4j.Logger;
import picocli.CommandLine;

/**
 * Displays dependency management tree of Maven Artifact.
 */
@CommandLine.Command(name = "dm-tree", description = "Displays dependency management tree of Maven Artifact")
@Mojo(name = "gav-dm-tree", requiresProject = false, threadSafe = true)
public class GavDmTreeMojo extends GavMojoSupport {
    /**
     * The GAV to show tree for.
     */
    @CommandLine.Parameters(index = "0", description = "The GAV to show dependency management tree for", arity = "1")
    @Parameter(property = "gav", required = true)
    private String gav;

    /**
     * Comma separated list of BOMs to apply.
     */
    @CommandLine.Option(
            names = {"--boms"},
            defaultValue = "",
            description = "Comma separated list of BOMs to apply")
    @Parameter(property = "boms")
    private String boms;

    /**
     * Set it {@code true} for verbose tree.
     */
    @CommandLine.Option(
            names = {"--verboseTree"},
            defaultValue = "false",
            description = "Make it true for verbose tree")
    @Parameter(property = "verboseTree", defaultValue = "false", required = true)
    private boolean verboseTree;

    @Override
    protected Result<CollectResult> doExecute(Logger output, ToolboxCommando toolboxCommando) throws Exception {
        return toolboxCommando.dmTree(toolboxCommando.loadGav(gav, slurp(boms)), verboseTree, output);
    }
}
