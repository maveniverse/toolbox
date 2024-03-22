/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin;

import eu.maveniverse.maven.toolbox.shared.ResolutionRoot;
import eu.maveniverse.maven.toolbox.shared.ResolutionScope;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "plugin-tree", threadSafe = true)
public class PluginTreeMojo extends ProjectMojoSupport {
    /**
     * The plugin key in the format {@code <groupId>:<artifactId>} to display tree for. If plugin is from "known"
     * groupId (as configured in settings.xml) it may be in format of {@code :<artifactId>} and this mojo will find it.
     * Finally, if plugin key is plain string like {@code "clean"}, this mojo will apply some heuristics to find it.
     */
    @Parameter(property = "pluginKey", required = true)
    private String pluginKey;

    /**
     * The resolution scope to display, accepted values are "runtime", "compile", "test", etc.
     */
    @Parameter(property = "scope", defaultValue = "runtime", required = true)
    private String scope;

    /**
     * Set it {@code true} for verbose tree.
     */
    @Parameter(property = "verboseTree", defaultValue = "false", required = true)
    private boolean verboseTree;

    @Override
    protected void doExecute(ToolboxCommando toolboxCommando) throws Exception {
        Plugin plugin = null;
        if (pluginKey == null || pluginKey.trim().isEmpty()) {
            throw new IllegalArgumentException("pluginKey must not be empty string");
        }
        if (pluginKey.startsWith(":")) {
            for (String pluginGroup : settings.getPluginGroups()) {
                plugin = mavenProject.getPlugin(pluginGroup + pluginKey);
                if (plugin != null) {
                    break;
                }
            }
        } else {
            plugin = mavenProject.getPlugin(pluginKey);
            if (plugin == null) {
                for (Plugin p : mavenProject.getBuildPlugins()) {
                    if (p.getKey().contains(pluginKey)) {
                        plugin = p;
                        break;
                    }
                }
            }
        }
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin not found");
        }
        ResolutionRoot root =
                toolboxCommando.loadGav(plugin.getGroupId() + ":" + plugin.getArtifactId() + ":" + plugin.getVersion());
        if (!plugin.getDependencies().isEmpty()) {
            root.getDependencies().addAll(toDependencies(plugin.getDependencies()));
        }
        toolboxCommando.tree(ResolutionScope.parse(scope), root, verboseTree, output);
    }
}
