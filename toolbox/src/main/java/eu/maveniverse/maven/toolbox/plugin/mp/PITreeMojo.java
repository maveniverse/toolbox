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
import org.eclipse.aether.collection.CollectResult;

/**
 * Displays project inheritance of Maven Projects.
 */
@Mojo(name = "pi-tree", threadSafe = true)
public class PITreeMojo extends MPMojoSupport {
    @Override
    protected Result<CollectResult> doExecute() throws Exception {
        return getToolboxCommando().projectInheritanceTree(getProjectLocator());
    }
}
