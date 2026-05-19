/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.mp;

import eu.maveniverse.maven.toolbox.plugin.MPMojoSupport;
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

/**
 * Displays dependency tree of Maven Project if it would be a direct dependency of some project.
 */
@Mojo(name = "tree-dd", threadSafe = true)
public class TreeDDMojo extends MPMojoSupport {
    /**
     * The resolution scope to display, accepted values are "runtime", "compile", "test", etc.
     */
    @Parameter(property = "scope", defaultValue = "runtime", required = true)
    private String scope;

    /**
     * The dependency scope to put project in, accepted values are "runtime", "compile", "test", etc.
     */
    @Parameter(property = "dependencyScope", defaultValue = "compile", required = true)
    private String dependencyScope;

    /**
     * The dependency matcher if you want to filter as eager as Lenny wants.
     */
    @Parameter(property = "dependencyMatcher", defaultValue = "any()", required = true)
    private String dependencyMatcher;

    /**
     * Set it {@code true} for verbose tree.
     */
    @Parameter(property = "verboseTree", defaultValue = "false", required = true)
    private boolean verboseTree;

    /**
     * Set it {@code true} for verbose tree nodes.
     */
    @Parameter(property = "verboseTreeNode", defaultValue = "false", required = true)
    private boolean verboseTreeNode;

    @Override
    protected Result<CollectResult> doExecute() throws Exception {
        ToolboxCommando toolboxCommando = getToolboxCommando();
        ResolutionRoot root = ResolutionRoot.ofNotLoaded(new DefaultArtifact("org.example:some-project:1.0.0"))
                .withDependencies(
                        List.of(new Dependency(projectAsResolutionRoot().getArtifact(), dependencyScope)))
                .build();
        return toolboxCommando.tree(
                ResolutionScope.parse(scope),
                root,
                verboseTree,
                verboseTreeNode,
                toolboxCommando.parseDependencyMatcherSpec(dependencyMatcher));
    }
}
