/*
 * Copyright (c) 2023-2024 Maveniverse Org.
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
import java.util.Map;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.collection.CollectResult;
import picocli.CommandLine;

/**
 * Displays dependency tree differences of two Maven Artifact.
 */
@CommandLine.Command(name = "tree-diff", description = "Displays dependency tree differences of two Maven Artifact")
@Mojo(name = "gav-tree-diff", requiresProject = false, threadSafe = true)
public class GavTreeDiffMojo extends GavMojoSupport {
    /**
     * The first GAV to show tree for.
     */
    @CommandLine.Parameters(index = "0", description = "The first GAV to show tree for", arity = "1")
    @Parameter(property = "gav1", required = true)
    private String gav1;

    /**
     * The second GAV to show tree for.
     */
    @CommandLine.Parameters(index = "1", description = "The second GAV to show tree for", arity = "1")
    @Parameter(property = "gav2", required = true)
    private String gav2;

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
     * Comma separated list of BOMs to apply to both.
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
    @CommandLine.Option(
            names = {"--dependencyMatcher"},
            defaultValue = "any()",
            description = "Dependency matcher spec")
    @Parameter(property = "dependencyMatcher", defaultValue = "any()", required = true)
    private String dependencyMatcher;

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
    protected Result<Map<CollectResult, CollectResult>> doExecute() throws Exception {
        ToolboxCommando toolboxCommando = getToolboxCommando();
        return toolboxCommando.treeDiff(
                ResolutionScope.parse(scope),
                toolboxCommando.loadGav(gav1, slurp(boms)),
                toolboxCommando.loadGav(gav2, slurp(boms)),
                verboseTree,
                toolboxCommando.parseDependencyMatcherSpec(dependencyMatcher));
    }
}
