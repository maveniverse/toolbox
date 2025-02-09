/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.mp;

import eu.maveniverse.maven.toolbox.plugin.MPMojoSupport;
import eu.maveniverse.maven.toolbox.shared.ResolutionRoot;
import eu.maveniverse.maven.toolbox.shared.Result;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import java.util.List;
import java.util.Map;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.version.Version;
import picocli.CommandLine;

/**
 * Lists available versions of Maven Project dependencies.
 */
@Mojo(name = "versions", threadSafe = true)
public class VersionsMojo extends MPMojoSupport {
    /**
     * The dependency matcher spec.
     */
    @Parameter(property = "depSpec", defaultValue = "any()")
    private String depSpec;

    /**
     * Artifact version matcher spec string, default is 'noSnapshotsAndPreviews()'.
     */
    @Parameter(property = "artifactVersionMatcherSpec", defaultValue = "noSnapshotsAndPreviews()")
    private String artifactVersionMatcherSpec;

    /**
     * Artifact version selector spec string to select the version from candidates, default is 'last()'.
     */
    @CommandLine.Option(
            names = {"--artifactVersionSelectorSpec"},
            defaultValue = "last()",
            description = "Artifact version selector spec (default 'last()')")
    @Parameter(property = "artifactVersionSelectorSpec", defaultValue = "last()")
    private String artifactVersionSelectorSpec;

    /**
     * Apply results to POM.
     */
    @Parameter(property = "applyToPom")
    private boolean applyToPom;

    @Override
    protected Result<Boolean> doExecute() throws Exception {
        ToolboxCommando toolboxCommando = getToolboxCommando();
        Result<Map<Artifact, List<Version>>> managedDependencies = toolboxCommando.versions(
                "managed dependencies",
                () ->
                        projectManagedDependenciesAsResolutionRoots(toolboxCommando.parseDependencyMatcherSpec(depSpec))
                                .stream()
                                .map(ResolutionRoot::getArtifact),
                toolboxCommando.parseArtifactVersionMatcherSpec(artifactVersionMatcherSpec),
                toolboxCommando.parseArtifactVersionSelectorSpec(artifactVersionSelectorSpec));
        Result<Map<Artifact, List<Version>>> dependencies = toolboxCommando.versions(
                "dependencies",
                () -> projectDependenciesAsResolutionRoots(toolboxCommando.parseDependencyMatcherSpec(depSpec)).stream()
                        .map(ResolutionRoot::getArtifact),
                toolboxCommando.parseArtifactVersionMatcherSpec(artifactVersionMatcherSpec),
                toolboxCommando.parseArtifactVersionSelectorSpec(artifactVersionSelectorSpec));

        if (applyToPom) {
            List<Artifact> managedDependenciesUpdates = toolboxCommando.calculateUpdates(
                    managedDependencies.getData().orElseThrow());
            List<Artifact> dependenciesUpdates =
                    toolboxCommando.calculateUpdates(dependencies.getData().orElseThrow());
            if (!managedDependenciesUpdates.isEmpty() || !dependenciesUpdates.isEmpty()) {
                try (ToolboxCommando.EditSession editSession =
                        toolboxCommando.createEditSession(mavenProject.getFile().toPath())) {
                    if (!managedDependenciesUpdates.isEmpty()) {
                        toolboxCommando.doManagedDependencies(
                                editSession, ToolboxCommando.Op.UPDATE, managedDependenciesUpdates::stream);
                    }
                    if (!dependenciesUpdates.isEmpty()) {
                        toolboxCommando.doDependencies(
                                editSession, ToolboxCommando.Op.UPDATE, dependenciesUpdates::stream);
                    }
                }
            }
        }
        return Result.success(true);
    }
}
