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
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import picocli.CommandLine;

/**
 * Collects and displays paths to matched artifacts in tree, if exists.
 */
@CommandLine.Command(
        name = "tree-dd-find",
        description = "Collects and displays paths to matched artifacts in tree, if exists")
@Mojo(name = "gav-tree-dd-find", requiresProject = false, threadSafe = true)
public class GavTreeDDFindMojo extends GavMojoSupport {
    /**
     * The GAV to show tree for.
     */
    @CommandLine.Parameters(index = "0", description = "The GAV to show tree for", arity = "1")
    @Parameter(property = "gav", required = true)
    private String gav;

    /**
     * The artifact matcher spec.
     */
    @CommandLine.Parameters(index = "1", description = "The artifact to show paths for", arity = "1")
    @Parameter(property = "artifactMatcherSpec", required = true)
    private String artifactMatcherSpec;

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
    @Parameter(property = "dependencyScope", defaultValue = "compile", required = true)
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
     * Set it {@code true} for verbose tree.
     */
    @CommandLine.Option(
            names = {"--verboseTree"},
            defaultValue = "false",
            description = "Set it true for verbose tree")
    @Parameter(property = "verboseTree", defaultValue = "false", required = true)
    private boolean verboseTree;

    /**
     * Set it {@code true} to have related management displayed.
     */
    @CommandLine.Option(
            names = {"--showManagement"},
            defaultValue = "false",
            description = "Set it true to have related management displayed")
    @Parameter(property = "showManagement", defaultValue = "false", required = true)
    private boolean showManagement;

    @Override
    protected Result<List<List<Artifact>>> doExecute() throws Exception {
        ToolboxCommando toolboxCommando = getToolboxCommando();
        ResolutionRoot root = ResolutionRoot.ofNotLoaded(new DefaultArtifact("org.example:some-project:1.0.0"))
                .withDependencies(List.of(new Dependency(new DefaultArtifact(gav), dependencyScope)))
                .withManagedDependencies(toolboxCommando.getToolboxResolver().importBOMs(slurp(boms)))
                .build();

        return toolboxCommando.treeFind(
                ResolutionScope.parse(scope),
                root,
                verboseTree,
                toolboxCommando.parseArtifactMatcherSpec(artifactMatcherSpec),
                showManagement);
    }
}
