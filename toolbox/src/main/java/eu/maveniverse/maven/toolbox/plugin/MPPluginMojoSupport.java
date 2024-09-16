/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.toolbox.shared.ResolutionRoot;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.InputLocationTracker;
import org.apache.maven.model.InputSource;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Profile;
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
        requireNonNull(model, "model");
        String modelId = model.getGroupId() + ":" + model.getArtifactId() + ":" + model.getVersion();
        return tracker -> {
            if (tracker != null) {
                InputLocation location = tracker.getLocation("");
                if (location != null) {
                    InputSource source = location.getSource();
                    return source != null && Objects.equals(source.getModelId(), modelId);
                }
            }
            return false;
        };
    }

    protected Function<Model, BuildBase> projectBuildBaseSelector() {
        return model -> {
            if (model != null) {
                return model.getBuild();
            }
            return null;
        };
    }

    protected Function<Model, BuildBase> profileBuildBaseSelector(String profileId) {
        requireNonNull(profileId, "profileId");
        return model -> {
            if (model != null) {
                for (Profile profile : model.getProfiles()) {
                    if (profileId.equals(profile.getId())) {
                        return profile.getBuild();
                    }
                }
            }
            return null;
        };
    }

    protected Function<BuildBase, List<Plugin>> buildManagedPluginsExtractor() {
        return build -> {
            if (build != null && build.getPluginManagement() != null) {
                return build.getPluginManagement().getPlugins();
            }
            return null;
        };
    }

    protected Function<BuildBase, List<Plugin>> buildPluginsExtractor() {
        return build -> {
            if (build != null) {
                return build.getPlugins();
            }
            return null;
        };
    }

    protected Function<Plugin, ResolutionRoot> pluginToResolutionRoot(ToolboxCommando toolboxCommando) {
        return plugin -> {
            if (plugin != null) {
                try {
                    ResolutionRoot root = toolboxCommando.loadGav(
                            plugin.getGroupId() + ":" + plugin.getArtifactId() + ":" + plugin.getVersion());
                    if (!plugin.getDependencies().isEmpty()) {
                        root = root.builder()
                                .withDependencies(toDependencies(plugin.getDependencies()))
                                .build();
                    }
                    return root;
                } catch (InvalidVersionSpecificationException
                        | VersionRangeResolutionException
                        | ArtifactDescriptorException e) {
                    throw new IllegalArgumentException(e);
                }
            }
            return null;
        };
    }

    protected ResolutionRoot pluginAsResolutionRoot(ToolboxCommando toolboxCommando, boolean mandatoryPluginKey) {
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
            // TODO: maybe warn the user?
            // logger.warn(
            //        "Plugin matching '{}' not found in project {} (packaging={})",
            //        pluginKey,
            //        mavenProject.getId(),
            //        mavenProject.getPackaging());
            return null;
        }
        return pluginToResolutionRoot(toolboxCommando).apply(plugin);
    }

    protected List<ResolutionRoot> allProjectManagedPluginsAsResolutionRoots(ToolboxCommando toolboxCommando) {
        return pluginResolutionRoots(
                projectBuildBaseSelector(),
                buildManagedPluginsExtractor(),
                definedInModel(mavenProject.getModel()),
                pluginToResolutionRoot(toolboxCommando));
    }

    protected List<ResolutionRoot> allProjectPluginsAsResolutionRoots(ToolboxCommando toolboxCommando) {
        return pluginResolutionRoots(
                projectBuildBaseSelector(),
                buildPluginsExtractor(),
                definedInModel(mavenProject.getModel()),
                pluginToResolutionRoot(toolboxCommando));
    }

    protected List<ResolutionRoot> allProfileManagedPluginsAsResolutionRoots(
            ToolboxCommando toolboxCommando, String profileId) {
        return pluginResolutionRoots(
                profileBuildBaseSelector(profileId),
                buildManagedPluginsExtractor(),
                definedInModel(mavenProject.getModel()),
                pluginToResolutionRoot(toolboxCommando));
    }

    protected List<ResolutionRoot> allProfilePluginsAsResolutionRoots(
            ToolboxCommando toolboxCommando, String profileId) {
        return pluginResolutionRoots(
                profileBuildBaseSelector(profileId),
                buildPluginsExtractor(),
                definedInModel(mavenProject.getModel()),
                pluginToResolutionRoot(toolboxCommando));
    }

    private <T> List<T> pluginResolutionRoots(
            Function<Model, BuildBase> selector,
            Function<BuildBase, List<Plugin>> extractor,
            Predicate<Plugin> predicate,
            Function<Plugin, T> transformer) {
        List<T> result = new ArrayList<>();
        List<Plugin> plugins = extractor.apply(selector.apply(mavenProject.getModel()));
        if (plugins != null) {
            for (Plugin plugin : plugins) {
                if (!predicate.test(plugin)) {
                    continue;
                }
                result.add(transformer.apply(plugin));
            }
        }
        return result;
    }
}
