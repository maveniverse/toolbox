/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.gav;

import static eu.maveniverse.maven.toolbox.shared.input.StringSlurper.slurp;

import eu.maveniverse.maven.toolbox.plugin.GavMojoSupport;
import eu.maveniverse.maven.toolbox.shared.ResolutionScope;
import eu.maveniverse.maven.toolbox.shared.Result;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.collection.CollectResult;
import picocli.CommandLine;

/**
 * Displays dirty tree of Maven Artifact.
 */
@CommandLine.Command(name = "dirty-tree", description = "Displays dirty tree of Maven Artifact")
@Mojo(name = "gav-dirty-tree", requiresProject = false, threadSafe = true)
public class GavDirtyTreeMojo extends GavMojoSupport {
    /**
     * The GAV to show tree for.
     */
    @CommandLine.Parameters(index = "0", description = "The GAV to show tree for", arity = "1")
    @Parameter(property = "gav", required = true)
    private String gav;

    /**
     * Resolution scope to resolve (default 'runtime').
     */
    @CommandLine.Option(
            names = {"--scope"},
            defaultValue = "runtime",
            description = "Resolution scope to resolve (default 'runtime')")
    @Parameter(property = "scope", defaultValue = "runtime", required = true)
    private String scope;

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
     * The dependency matcher if you want to filter as eager as Lenny wants.
     */
    @Parameter(property = "dependencyMatcher", defaultValue = "any()", required = true)
    private String dependencyMatcher;

    /**
     * The level up to you want to see dirty tree. Note: keep it low, otherwise this call is OOM prone.
     * Default: 0 (direct siblings only)
     */
    @CommandLine.Option(
            names = {"--dirtyLevel"},
            defaultValue = "0",
            description = "Set the level you want to see dirty tree up to")
    @Parameter(property = "dirtyLevel", defaultValue = "0", required = true)
    private int dirtyLevel;

    /**
     * Set it {@code true} to conflict resolve the tree.
     */
    @CommandLine.Option(
            names = {"--conflictResolve"},
            defaultValue = "false",
            description = "Make it true tp conflict resolve the tree")
    @Parameter(property = "conflictResolve", defaultValue = "false", required = true)
    private boolean conflictResolve;

    @Override
    protected Result<CollectResult> doExecute() throws Exception {
        ToolboxCommando toolboxCommando = getToolboxCommando();
        return toolboxCommando.dirtyTree(
                ResolutionScope.parse(scope),
                toolboxCommando.loadGav(gav, slurp(boms)),
                dirtyLevel,
                conflictResolve,
                toolboxCommando.parseDependencyMatcherSpec(dependencyMatcher));
    }
}
