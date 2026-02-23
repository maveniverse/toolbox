/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.mp;

import static eu.maveniverse.maven.toolbox.shared.internal.domtrip.DOMTripUtils.toDomTrip;

import eu.maveniverse.maven.toolbox.plugin.MPPluginMojoSupport;
import eu.maveniverse.maven.toolbox.shared.ArtifactMatcher;
import eu.maveniverse.maven.toolbox.shared.ArtifactVersionMatcher;
import eu.maveniverse.maven.toolbox.shared.ArtifactVersionSelector;
import eu.maveniverse.maven.toolbox.shared.DependencyMatcher;
import eu.maveniverse.maven.toolbox.shared.ResolutionRoot;
import eu.maveniverse.maven.toolbox.shared.Result;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.version.Version;

/**
 * Lists available updates to everything of Maven Project (including core extensions).
 */
@Mojo(name = "versions", threadSafe = true)
public class VersionsMojo extends MPPluginMojoSupport {
    /**
     * Artifact version matcher spec string, default is 'any()'.
     */
    @Parameter(property = "artifactVersionMatcherSpec", defaultValue = "any()")
    private String artifactVersionMatcherSpec;

    /**
     * Artifact version selector spec string to select the version from candidates, default is 'contextualSnapshotsAndPreviews()'.
     */
    @Parameter(property = "artifactVersionSelectorSpec", defaultValue = "contextualSnapshotsAndPreviews()")
    private String artifactVersionSelectorSpec;

    /**
     * Perform check for effective POM (including all DM entries imported from BOMs as well).
     */
    @Parameter(property = "effective", defaultValue = "false")
    private boolean effective;

    /**
     * Apply results to POM.
     */
    @Parameter(property = "apply")
    private boolean apply;

    @Override
    protected Result<Boolean> doExecute() throws Exception {
        ToolboxCommando toolboxCommando = getToolboxCommando();
        ArtifactVersionMatcher artifactVersionMatcher =
                toolboxCommando.parseArtifactVersionMatcherSpec(artifactVersionMatcherSpec);
        ArtifactVersionSelector artifactVersionSelector =
                toolboxCommando.parseArtifactVersionSelectorSpec(artifactVersionSelectorSpec);

        ToolboxCommando.ExtensionsScope extensionsScope = ToolboxCommando.ExtensionsScope.PROJECT;

        Path extensionsXml;
        if (mavenSession.getRequest().isProjectPresent()) {
            extensionsXml = mavenSession
                    .getTopLevelProject()
                    .getBasedir()
                    .toPath()
                    .resolve(".mvn")
                    .resolve("extensions.xml");
        } else {
            extensionsXml = Path.of(".mvn").resolve("extensions.xml");
        }

        Result<List<Artifact>> coreExtensionsDiscovered = toolboxCommando.listExtensions(extensionsXml);
        Result<Map<Artifact, List<Version>>> coreExtensions = coreExtensionsDiscovered.isSuccess()
                ? toolboxCommando.versions(
                        extensionsScope.name().toLowerCase(Locale.ROOT) + " extensions",
                        () -> coreExtensionsDiscovered.getData().orElseThrow().stream(),
                        artifactVersionMatcher,
                        artifactVersionSelector)
                : Result.success(Map.of());

        Result<Map<Artifact, List<Version>>> extensions = toolboxCommando.versions(
                "extensions",
                () -> allProjectExtensionsAsResolutionRoots(toolboxCommando).stream()
                        .map(ResolutionRoot::getArtifact),
                artifactVersionMatcher,
                artifactVersionSelector);

        AtomicReference<Artifact> parentArtifact = new AtomicReference<>(null);
        if (mavenSession.getRequest().isProjectPresent()
                && mavenProject.getModel().getParent() != null) {
            parentArtifact.set(new DefaultArtifact(
                    mavenProject.getModel().getParent().getGroupId(),
                    mavenProject.getModel().getParent().getArtifactId(),
                    "pom",
                    mavenProject.getModel().getParent().getVersion()));
        }

        Result<Map<Artifact, List<Version>>> parents = null;
        if (parentArtifact.get() != null) {
            parents = toolboxCommando.versions(
                    "parent", () -> Stream.of(parentArtifact.get()), artifactVersionMatcher, artifactVersionSelector);
        }
        Result<Map<Artifact, List<Version>>> managedPlugins = toolboxCommando.versions(
                "managed plugins",
                () -> allProjectManagedPluginsAsResolutionRoots(toolboxCommando).stream()
                        .map(ResolutionRoot::getArtifact)
                        .filter(ArtifactMatcher.any()),
                artifactVersionMatcher,
                artifactVersionSelector);
        Result<Map<Artifact, List<Version>>> plugins = toolboxCommando.versions(
                "plugins",
                () -> allProjectPluginsAsResolutionRoots(toolboxCommando).stream()
                        .map(ResolutionRoot::getArtifact)
                        .filter(ArtifactMatcher.any()),
                artifactVersionMatcher,
                artifactVersionSelector);

        Result<Map<Artifact, List<Version>>> managedDependencies = toolboxCommando.versions(
                "managed dependencies",
                () -> projectManagedDependenciesAsResolutionRoots(effective, DependencyMatcher.any()).stream()
                        .map(ResolutionRoot::getArtifact),
                artifactVersionMatcher,
                artifactVersionSelector);
        Result<Map<Artifact, List<Version>>> dependencies = toolboxCommando.versions(
                "dependencies",
                () -> projectDependenciesAsResolutionRoots(DependencyMatcher.any()).stream()
                        .map(ResolutionRoot::getArtifact),
                artifactVersionMatcher,
                artifactVersionSelector);

        if (apply) {
            List<Artifact> coreExtensionsUpdates =
                    toolboxCommando.calculateUpdates(coreExtensions.getData().orElseThrow(), artifactVersionSelector);
            if (!coreExtensionsUpdates.isEmpty()) {
                try (ToolboxCommando.EditSession editSession = toolboxCommando.createEditSession(extensionsXml)) {
                    toolboxCommando.editExtensions(
                            editSession, ToolboxCommando.Op.UPDATE, coreExtensionsUpdates::stream);
                }
            }

            AtomicReference<List<Artifact>> parentsUpdates = new AtomicReference<>(null);
            if (parents != null) {
                parentsUpdates.set(
                        toolboxCommando.calculateUpdates(parents.getData().orElseThrow(), artifactVersionSelector));
            }
            List<Artifact> extensionUpdates =
                    toolboxCommando.calculateUpdates(extensions.getData().orElseThrow(), artifactVersionSelector);
            List<Artifact> managedPluginsUpdates =
                    toolboxCommando.calculateUpdates(managedPlugins.getData().orElseThrow(), artifactVersionSelector);
            List<Artifact> pluginsUpdates =
                    toolboxCommando.calculateUpdates(plugins.getData().orElseThrow(), artifactVersionSelector);
            List<Artifact> managedDependenciesUpdates = toolboxCommando.calculateUpdates(
                    managedDependencies.getData().orElseThrow(), artifactVersionSelector);
            List<Artifact> dependenciesUpdates =
                    toolboxCommando.calculateUpdates(dependencies.getData().orElseThrow(), artifactVersionSelector);
            if ((parentsUpdates.get() != null && !parentsUpdates.get().isEmpty()) && !extensionUpdates.isEmpty()
                    || !managedPluginsUpdates.isEmpty()
                    || !pluginsUpdates.isEmpty()
                    || !managedDependenciesUpdates.isEmpty()
                    || !dependenciesUpdates.isEmpty()) {
                try (ToolboxCommando.EditSession editSession =
                        toolboxCommando.createEditSession(mavenProject.getFile().toPath())) {
                    if (parentsUpdates.get() != null && !parentsUpdates.get().isEmpty()) {
                        toolboxCommando.editPom(editSession, Collections.singletonList(s -> s.parent()
                                .updateParent(
                                        false, toDomTrip(parentsUpdates.get().get(0)))));
                    }
                    if (!extensionUpdates.isEmpty()) {
                        toolboxCommando.editPom(
                                editSession,
                                ToolboxCommando.PomOpSubject.EXTENSIONS,
                                ToolboxCommando.Op.UPDATE,
                                extensionUpdates::stream);
                    }
                    if (!managedPluginsUpdates.isEmpty()) {
                        toolboxCommando.editPom(
                                editSession,
                                ToolboxCommando.PomOpSubject.MANAGED_PLUGINS,
                                ToolboxCommando.Op.UPDATE,
                                managedPluginsUpdates::stream);
                    }
                    if (!pluginsUpdates.isEmpty()) {
                        toolboxCommando.editPom(
                                editSession,
                                ToolboxCommando.PomOpSubject.PLUGINS,
                                ToolboxCommando.Op.UPDATE,
                                pluginsUpdates::stream);
                    }
                    if (!managedDependenciesUpdates.isEmpty()) {
                        toolboxCommando.editPom(
                                editSession,
                                ToolboxCommando.PomOpSubject.MANAGED_DEPENDENCIES,
                                ToolboxCommando.Op.UPDATE,
                                managedDependenciesUpdates::stream);
                    }
                    if (!dependenciesUpdates.isEmpty()) {
                        toolboxCommando.editPom(
                                editSession,
                                ToolboxCommando.PomOpSubject.DEPENDENCIES,
                                ToolboxCommando.Op.UPDATE,
                                dependenciesUpdates::stream);
                    }
                }
            }
        }
        return Result.success(true);
    }
}
