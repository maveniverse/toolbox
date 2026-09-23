/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.toolbox.shared.ArtifactVersionSelector;
import eu.maveniverse.maven.toolbox.shared.ResolutionRoot;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
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
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;

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

    /**
     * Collects plugins from the main build AND from all profile builds, keyed by scope.
     * The map key is {@code null} for the main {@code <build>} section, or a profile id string
     * for plugins declared inside {@code <profiles>/<profile>/<build>/<plugins>}.
     *
     * <p>Insertion order is preserved: main build first, then profiles in declaration order.</p>
     *
     * @param toolboxCommando the toolbox commando
     * @return ordered map of scope → resolution roots (null key = main build)
     */
    protected Map<String, List<ResolutionRoot>> allProjectPluginsAsResolutionRootsPerScope(
            ToolboxCommando toolboxCommando) {
        return allProjectPluginsAsResolutionRootsPerScope(toolboxCommando, mavenProject);
    }

    /**
     * Collects managed plugins from the main build AND from all profile builds, keyed by scope.
     *
     * @param toolboxCommando the toolbox commando
     * @return ordered map of scope → resolution roots (null key = main build)
     */
    protected Map<String, List<ResolutionRoot>> allProjectManagedPluginsAsResolutionRootsPerScope(
            ToolboxCommando toolboxCommando) {
        return allProjectManagedPluginsAsResolutionRootsPerScope(toolboxCommando, mavenProject);
    }

    protected Map<String, List<ResolutionRoot>> allProjectPluginsAsResolutionRootsPerScope(
            ToolboxCommando toolboxCommando, MavenProject mavenProject) {
        Map<String, List<ResolutionRoot>> result = new LinkedHashMap<>();
        // Main build
        List<ResolutionRoot> mainPlugins = allProjectPluginsAsResolutionRoots(toolboxCommando, mavenProject);
        if (!mainPlugins.isEmpty()) {
            result.put(null, mainPlugins);
        }
        // Profile builds
        for (Profile profile : mavenProject.getModel().getProfiles()) {
            List<ResolutionRoot> profilePlugins = selectExtractResolutionRoots(
                    profileBuildBaseSelector(profile.getId()),
                    buildPluginsExtractor(),
                    definedInModel(mavenProject.getModel()),
                    pluginToResolutionRoot(toolboxCommando),
                    mavenProject);
            if (!profilePlugins.isEmpty()) {
                result.put(profile.getId(), profilePlugins);
            }
        }
        return result;
    }

    protected Map<String, List<ResolutionRoot>> allProjectManagedPluginsAsResolutionRootsPerScope(
            ToolboxCommando toolboxCommando, MavenProject mavenProject) {
        Map<String, List<ResolutionRoot>> result = new LinkedHashMap<>();
        // Main build
        List<ResolutionRoot> mainPlugins = allProjectManagedPluginsAsResolutionRoots(toolboxCommando, mavenProject);
        if (!mainPlugins.isEmpty()) {
            result.put(null, mainPlugins);
        }
        // Profile builds
        for (Profile profile : mavenProject.getModel().getProfiles()) {
            List<ResolutionRoot> profilePlugins = selectExtractResolutionRoots(
                    profileBuildBaseSelector(profile.getId()),
                    buildManagedPluginsExtractor(),
                    definedInModel(mavenProject.getModel()),
                    pluginToResolutionRoot(toolboxCommando),
                    mavenProject);
            if (!profilePlugins.isEmpty()) {
                result.put(profile.getId(), profilePlugins);
            }
        }
        return result;
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

    /**
     * Applies plugin version updates, routing each artifact to the correct POM scope (main build or a profile).
     *
     * <p>For each scope, only the artifacts declared in that scope are considered, and their updates are
     * computed from the per-artifact version data that was already resolved. This prevents a same-GA plugin
     * declared in two different scopes (e.g. main build at 1.0 and a profile at 2.0) from having one
     * scope's target version written into the other.</p>
     *
     * @param toolboxCommando     the toolbox commando
     * @param editSession         the active edit session
     * @param allVersions         the resolved version map for all artifacts (across all scopes)
     * @param rootsPerScope       plugins grouped by scope (null key = main build, non-null = profile id)
     * @param artifactVersionSelector the version selector
     * @param subject             the POM section to update ({@code PLUGINS} or {@code MANAGED_PLUGINS})
     */
    protected void applyPluginUpdatesPerScope(
            ToolboxCommando toolboxCommando,
            ToolboxCommando.EditSession editSession,
            Map<Artifact, List<Version>> allVersions,
            Map<String, List<ResolutionRoot>> rootsPerScope,
            ArtifactVersionSelector artifactVersionSelector,
            ToolboxCommando.PomOpSubject subject)
            throws Exception {
        for (Map.Entry<String, List<ResolutionRoot>> entry : rootsPerScope.entrySet()) {
            String scopeProfileId = entry.getKey(); // null = main build
            List<ResolutionRoot> scopeRoots = entry.getValue();

            // Build a version sub-map containing only artifacts that belong to this scope,
            // matched by exact artifact identity (groupId + artifactId + version).
            // This prevents a same-GA plugin at a different version in another scope from
            // contributing an update to this scope.
            Map<Artifact, List<Version>> scopeVersions = allVersions.entrySet().stream()
                    .filter(e -> scopeRoots.stream()
                            .anyMatch(root -> root.getArtifact().equals(e.getKey())))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            List<Artifact> scopeUpdates = toolboxCommando.calculateUpdates(scopeVersions, artifactVersionSelector);

            if (!scopeUpdates.isEmpty()) {
                toolboxCommando.editPom(
                        editSession, subject, ToolboxCommando.Op.UPDATE, scopeUpdates::stream, scopeProfileId);
            }
        }
    }
}
