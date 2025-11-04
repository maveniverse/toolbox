/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.mp;

import eu.maveniverse.maven.toolbox.plugin.MPPluginMojoSupport;
import eu.maveniverse.maven.toolbox.shared.ResolutionRoot;
import eu.maveniverse.maven.toolbox.shared.ResolutionScope;
import eu.maveniverse.maven.toolbox.shared.Result;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Collects project given plugin and output its dependency tree.
 */
@Mojo(name = "plugin-tree", threadSafe = true)
public class PluginTreeMojo extends MPPluginMojoSupport {
    /**
     * The resolution scope to display, accepted values are "runtime", "compile", "test", etc.
     */
    @Parameter(property = "scope", defaultValue = "runtime", required = true)
    private String scope;

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
    protected Result<Boolean> doExecute() throws Exception {
        ToolboxCommando toolboxCommando = getToolboxCommando();
        ResolutionRoot root = pluginAsResolutionRoot(toolboxCommando, false);
        if (root != null) {
            toolboxCommando.tree(
                    ResolutionScope.parse(scope),
                    root,
                    verboseTree,
                    verboseTreeNode,
                    toolboxCommando.parseDependencyMatcherSpec(dependencyMatcher));
        } else {
            for (ResolutionRoot resolutionRoot : allProjectPluginsAsResolutionRoots(toolboxCommando)) {
                toolboxCommando.tree(
                        ResolutionScope.parse(scope),
                        resolutionRoot,
                        verboseTree,
                        verboseTreeNode,
                        toolboxCommando.parseDependencyMatcherSpec(dependencyMatcher));
            }
        }
        return Result.success(true);
    }
}
