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
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.InputLocationTracker;
import org.apache.maven.model.InputSource;
import org.apache.maven.model.Model;
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

    protected <T extends InputLocationTracker> Predicate<T> definedInModel(Model model) {
        String modelId = model.getGroupId() + ":" + model.getArtifactId() + ":" + model.getVersion();
        return dependency -> {
            InputLocation location = dependency.getLocation("");
            if (location != null) {
                InputSource source = location.getSource();
                return source != null && !Objects.equals(source.getModelId(), modelId);
            }
            return false;
        };
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
            logger.warn(
                    "Plugin matching '{}' not found in project {} (packaging={})",
                    pluginKey,
                    mavenProject.getId(),
                    mavenProject.getPackaging());
            return null;
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
        return pluginResolutionRoots(
                toolboxCommando,
                m -> {
                    if (m.getBuild() != null) {
                        return m.getBuild().getPlugins();
                    } else {
                        return null;
                    }
                },
                definedInModel(mavenProject.getModel()));
    }

    protected List<ResolutionRoot> allManagedPluginsAsResolutionRoots(ToolboxCommando toolboxCommando)
            throws InvalidVersionSpecificationException, VersionRangeResolutionException, ArtifactDescriptorException {
        return pluginResolutionRoots(
                toolboxCommando,
                m -> {
                    if (m.getBuild() != null && m.getBuild().getPluginManagement() != null) {
                        return m.getBuild().getPluginManagement().getPlugins();
                    } else {
                        return null;
                    }
                },
                definedInModel(mavenProject.getModel()));
    }

    protected List<ResolutionRoot> pluginResolutionRoots(
            ToolboxCommando toolboxCommando, Function<Model, List<Plugin>> selector, Predicate<Plugin> pluginPredicate)
            throws InvalidVersionSpecificationException, VersionRangeResolutionException, ArtifactDescriptorException {
        List<ResolutionRoot> roots = new ArrayList<>();
        Model model = mavenProject.getModel();
        List<Plugin> plugins = selector.apply(model);
        if (plugins != null) {
            for (Plugin plugin : plugins) {
                if (!pluginPredicate.test(plugin)) {
                    continue;
                }
                ResolutionRoot root = toolboxCommando.loadGav(
                        plugin.getGroupId() + ":" + plugin.getArtifactId() + ":" + plugin.getVersion());
                if (!plugin.getDependencies().isEmpty()) {
                    root = root.builder()
                            .withDependencies(toDependencies(plugin.getDependencies()))
                            .build();
                }
                roots.add(root);
            }
        }
        return roots;
    }
}
