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
 * Displays project interdependencies of Maven Projects.
 */
@Mojo(name = "project-dependency-tree", threadSafe = true)
public class ProjectDependencyTreeMojo extends MPMojoSupport {

    /**
     * Set it {@code true} for verbose tree.
     */
    @Parameter(property = "showExternal", defaultValue = "false", required = true)
    private boolean showExternal;

    @Override
    protected Result<CollectResult> doExecute() throws Exception {
        return getToolboxCommando().projectDependencyTree(getReactorLocator(), showExternal);
    }
}
