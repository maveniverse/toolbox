/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.gav;

import eu.maveniverse.maven.toolbox.plugin.GavMojoSupport;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * List available plugins.
 */
@Mojo(name = "gav-list-available-plugins", requiresProject = false, threadSafe = true)
public class GavListAvailablePluginsMojo extends GavMojoSupport {
    /**
     * The groupIds to list.
     */
    @Parameter(property = "groupIds")
    private String groupIds;

    @Override
    protected boolean doExecute(ToolboxCommando toolboxCommando) throws Exception {
        if (groupIds == null || groupIds.trim().isEmpty()) {
            groupIds = String.join(",", settings.getPluginGroups());
        }
        return toolboxCommando.listAvailablePlugins(csv(groupIds), output);
    }
}
