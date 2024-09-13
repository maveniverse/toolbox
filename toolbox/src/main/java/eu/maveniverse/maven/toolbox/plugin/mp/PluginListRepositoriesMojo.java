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
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;

/**
 * Resolves transitively a project given plugin and outputs used repositories.
 */
@Mojo(name = "plugin-list-repositories", threadSafe = true)
public final class PluginListRepositoriesMojo extends MPPluginMojoSupport {
    /**
     * The resolution scope to display, accepted values are "runtime", "compile", "test", etc.
     */
    @Parameter(property = "scope", defaultValue = "runtime", required = true)
    private String scope;

    @Override
    protected boolean doExecute(Logger output, ToolboxCommando toolboxCommando) throws Exception {
        ResolutionRoot root = pluginAsResolutionRoot(toolboxCommando, true);
        if (root != null) {
            return toolboxCommando.listRepositories(ResolutionScope.parse(scope), "plugin", root, output);
        } else {
            return true;
        }
    }
}
