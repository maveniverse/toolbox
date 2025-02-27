/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.mp;

import eu.maveniverse.maven.toolbox.plugin.MPMojoSupport;
import eu.maveniverse.maven.toolbox.shared.Result;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.collection.CollectResult;

/**
 * Displays project inheritance of Maven Projects.
 */
@Mojo(name = "parent-child-tree", aggregator = true, threadSafe = true)
public class ParentChildTreeMojo extends MPMojoSupport {
    /**
     * Set the project selector, like {@code -rf} Maven command uses it, can be {@code :A}, {@code G:A}. The selector
     * string must match ONE project within reactor, otherwise (matches 0 or more than 1) it will fail.
     */
    @Parameter(property = "selector")
    private String selector;

    @Override
    protected Result<CollectResult> doExecute() throws Exception {
        return getToolboxCommando().parentChildTree(getReactorLocator(selector));
    }
}
