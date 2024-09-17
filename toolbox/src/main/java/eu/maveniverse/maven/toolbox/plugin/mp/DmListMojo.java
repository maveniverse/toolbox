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
import java.util.List;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.graph.Dependency;

/**
 * Displays dependency management list of Maven Project.
 */
@Mojo(name = "dm-list", threadSafe = true)
public class DmListMojo extends MPMojoSupport {
    /**
     * Set it {@code true} for verbose list.
     */
    @Parameter(property = "verboseList", defaultValue = "false", required = true)
    private boolean verboseList;

    @Override
    protected Result<List<Dependency>> doExecute() throws Exception {
        return getToolboxCommando().dmList(projectAsResolutionRoot(), verboseList);
    }
}
