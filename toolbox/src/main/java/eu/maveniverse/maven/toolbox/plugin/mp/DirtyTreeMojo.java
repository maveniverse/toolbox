/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.mp;

import eu.maveniverse.maven.toolbox.plugin.MPMojoSupport;
import eu.maveniverse.maven.toolbox.shared.ResolutionScope;
import eu.maveniverse.maven.toolbox.shared.Result;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.collection.CollectResult;

/**
 * Displays dependency dirty tree of Maven Project.
 */
@Mojo(name = "dirty-tree", threadSafe = true)
public class DirtyTreeMojo extends MPMojoSupport {
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
     * The level up to you want to see dirty tree. Note: keep it low, otherwise this call is OOM pronect.
     * Default: 3
     */
    @Parameter(property = "dirtyLevel", defaultValue = "3", required = true)
    private int dirtyLevel;

    @Override
    protected Result<CollectResult> doExecute() throws Exception {
        ToolboxCommando toolboxCommando = getToolboxCommando();
        return toolboxCommando.dirtyTree(
                ResolutionScope.parse(scope),
                projectAsResolutionRoot(),
                dirtyLevel,
                verboseTree,
                toolboxCommando.parseDependencyMatcherSpec(dependencyMatcher));
    }
}
