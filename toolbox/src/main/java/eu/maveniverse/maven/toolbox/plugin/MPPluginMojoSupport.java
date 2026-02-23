/*
 * Copyright (c) 2023-2026 Maveniverse Org.
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
import org.apache.maven.model.Build;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Extension;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.InputLocationTracker;
import org.apache.maven.model.InputSource;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Profile;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
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

    protected Function<Model, Build> projectBuildSelector() {
        return model -> {
            if (model != null) {
                return model.getBuild();
            }
            return null;
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

    protected Function<Build, List<Extension>> buildExtensionsExtractor() {
        return build -> {
            if (build != null) {
                return build.getExtensions();
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

    protected Function<Extension, ResolutionRoot> extensionToResolutionRoot(ToolboxCommando toolboxCommando) {
        return extension -> {
            if (extension != null) {
                try {
                    return toolboxCommando.loadGav(
                            extension.getGroupId() + ":" + extension.getArtifactId() + ":" + extension.getVersion());
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
            for (String pluginGroup : mojoSettings.getPluginGroups()) {
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
        return allProjectManagedPluginsAsResolutionRoots(toolboxCommando, mavenProject);
    }

    protected List<ResolutionRoot> allProjectPluginsAsResolutionRoots(ToolboxCommando toolboxCommando) {
        return allProjectPluginsAsResolutionRoots(toolboxCommando, mavenProject);
    }

    protected List<ResolutionRoot> allProjectExtensionsAsResolutionRoots(ToolboxCommando toolboxCommando) {
        return allProjectExtensionsAsResolutionRoots(toolboxCommando, mavenProject);
    }

    protected List<ResolutionRoot> allProjectManagedPluginsAsResolutionRoots(
            ToolboxCommando toolboxCommando, MavenProject mavenProject) {
        return selectExtractResolutionRoots(
                projectBuildBaseSelector(),
                buildManagedPluginsExtractor(),
                definedInModel(mavenProject.getModel()),
                pluginToResolutionRoot(toolboxCommando),
                mavenProject);
    }

    protected List<ResolutionRoot> allProjectPluginsAsResolutionRoots(
            ToolboxCommando toolboxCommando, MavenProject mavenProject) {
        return selectExtractResolutionRoots(
                projectBuildBaseSelector(),
                buildPluginsExtractor(),
                definedInModel(mavenProject.getModel()),
                pluginToResolutionRoot(toolboxCommando),
                mavenProject);
    }

    protected List<ResolutionRoot> allProjectExtensionsAsResolutionRoots(
            ToolboxCommando toolboxCommando, MavenProject mavenProject) {
        return selectExtractResolutionRoots(
                projectBuildSelector(),
                buildExtensionsExtractor(),
                definedInModel(mavenProject.getModel()),
                extensionToResolutionRoot(toolboxCommando),
                mavenProject);
    }

    protected List<ResolutionRoot> allManagedPluginsAsResolutionRoots(
            ToolboxCommando toolboxCommando, MavenProject mavenProject) {
        return selectExtractResolutionRoots(
                projectBuildBaseSelector(),
                buildManagedPluginsExtractor(),
                p -> true,
                pluginToResolutionRoot(toolboxCommando),
                mavenProject);
    }

    protected List<ResolutionRoot> allPluginsAsResolutionRoots(
            ToolboxCommando toolboxCommando, MavenProject mavenProject) {
        return selectExtractResolutionRoots(
                projectBuildBaseSelector(),
                buildPluginsExtractor(),
                p -> true,
                pluginToResolutionRoot(toolboxCommando),
                mavenProject);
    }

    protected List<ResolutionRoot> allExtensionsAsResolutionRoots(
            ToolboxCommando toolboxCommando, MavenProject mavenProject) {
        return selectExtractResolutionRoots(
                projectBuildSelector(),
                buildExtensionsExtractor(),
                p -> true,
                extensionToResolutionRoot(toolboxCommando),
                mavenProject);
    }

    protected List<ResolutionRoot> allProfileManagedPluginsAsResolutionRoots(
            ToolboxCommando toolboxCommando, String profileId) {
        return selectExtractResolutionRoots(
                profileBuildBaseSelector(profileId),
                buildManagedPluginsExtractor(),
                definedInModel(mavenProject.getModel()),
                pluginToResolutionRoot(toolboxCommando),
                mavenProject);
    }

    protected List<ResolutionRoot> allProfilePluginsAsResolutionRoots(
            ToolboxCommando toolboxCommando, String profileId) {
        return selectExtractResolutionRoots(
                profileBuildBaseSelector(profileId),
                buildPluginsExtractor(),
                definedInModel(mavenProject.getModel()),
                pluginToResolutionRoot(toolboxCommando),
                mavenProject);
    }

    private <T, B extends BuildBase, S> List<T> selectExtractResolutionRoots(
            Function<Model, B> selector,
            Function<B, List<S>> extractor,
            Predicate<S> predicate,
            Function<S, T> transformer,
            MavenProject mavenProject) {
        List<T> result = new ArrayList<>();
        List<S> subjects = extractor.apply(selector.apply(mavenProject.getModel()));
        if (subjects != null) {
            for (S subject : subjects) {
                if (!predicate.test(subject)) {
                    continue;
                }
                result.add(transformer.apply(subject));
            }
        }
        return result;
    }
}
