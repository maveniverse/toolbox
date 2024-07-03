/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin;

import eu.maveniverse.maven.toolbox.shared.ResolutionRoot;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.version.InvalidVersionSpecificationException;

/**
 * Support class for "project aware" Mojos dealing with plugins.
 */
public abstract class MPPluginMojoSupport extends MPMojoSupport {
    /**
     * The plugin key in the format {@code <groupId>:<artifactId>} to display tree for. If plugin is from "known"
     * groupId (as configured in settings.xml) it may be in format of {@code :<artifactId>} and this mojo will find it.
     * Finally, if plugin key is plain string like {@code "clean"}, this mojo will apply some heuristics to find it.
     */
    @Parameter(property = "pluginKey")
    private String pluginKey;

    protected ResolutionRoot pluginAsResolutionRoot(ToolboxCommando toolboxCommando) throws Exception {
        return pluginAsResolutionRoot(toolboxCommando, true);
    }

    protected ResolutionRoot pluginAsResolutionRoot(ToolboxCommando toolboxCommando, boolean mandatoryPluginKey)
            throws Exception {
        Plugin plugin = null;
        if (pluginKey == null || pluginKey.trim().isEmpty()) {
            if (mandatoryPluginKey) {
                throw new IllegalArgumentException("Parameter 'pluginKey' must be set");
            } else {
                return null;
            }
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
        return root;
    }

    protected List<ResolutionRoot> allPluginsAsResolutionRoots(ToolboxCommando toolboxCommando)
            throws InvalidVersionSpecificationException, VersionRangeResolutionException, ArtifactDescriptorException {
        List<ResolutionRoot> roots = new ArrayList<>();
        for (Plugin plugin : mavenProject.getBuildPlugins()) {
            ResolutionRoot root = toolboxCommando.loadGav(
                    plugin.getGroupId() + ":" + plugin.getArtifactId() + ":" + plugin.getVersion());
            if (!plugin.getDependencies().isEmpty()) {
                root.getDependencies().addAll(toDependencies(plugin.getDependencies()));
            }
            roots.add(root);
        }
        return roots;
    }
}
