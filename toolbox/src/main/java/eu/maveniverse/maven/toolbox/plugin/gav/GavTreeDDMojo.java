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
import eu.maveniverse.maven.toolbox.shared.ResolutionRoot;
import eu.maveniverse.maven.toolbox.shared.ResolutionScope;
import eu.maveniverse.maven.toolbox.shared.Result;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import java.util.List;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.graph.Dependency;
import picocli.CommandLine;

/**
 * Displays dependency tree of Maven Artifact if it would be a direct dependency of some project.
 */
@CommandLine.Command(name = "tree-dd", description = "Displays dependency tree of Maven Artifact as a dependency")
@Mojo(name = "gav-tree-dd", requiresProject = false, threadSafe = true)
public class GavTreeDDMojo extends GavMojoSupport {
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
     * Resolution scope to resolve (default 'compile').
     */
    @CommandLine.Option(
            names = {"--dependencyScope"},
            defaultValue = "compile",
            description = "Resolution scope of dependency GAV (default 'compile')")
    @Parameter(property = "scope", defaultValue = "compile", required = true)
    private String dependencyScope;

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

    /**
     * Set it {@code true} for verbose tree node.
     */
    @CommandLine.Option(
            names = {"--verboseTreeNode"},
            defaultValue = "false",
            description = "Make it true for verbose tree node")
    @Parameter(property = "verboseTreeNode", defaultValue = "false", required = true)
    private boolean verboseTreeNode;

    @Override
    protected Result<CollectResult> doExecute() throws Exception {
        ToolboxCommando toolboxCommando = getToolboxCommando();
        ResolutionRoot root = ResolutionRoot.ofNotLoaded(new DefaultArtifact("org.example:some-project:1.0.0"))
                .withDependencies(List.of(new Dependency(new DefaultArtifact(gav), dependencyScope)))
                .withManagedDependencies(toolboxCommando.getToolboxResolver().importBOMs(slurp(boms)))
                .build();
        return toolboxCommando.tree(
                ResolutionScope.parse(scope),
                root,
                verboseTree,
                verboseTreeNode,
                toolboxCommando.parseDependencyMatcherSpec(dependencyMatcher));
    }
}
