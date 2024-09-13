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
import eu.maveniverse.maven.toolbox.shared.ResolutionScope;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;

/**
 * Lists available versions of Maven Project dependencies.
 */
@Mojo(name = "versions", threadSafe = true)
public class VersionsMojo extends MPMojoSupport {
    /**
     * Resolution scope to resolve (default 'test').
     */
    @Parameter(property = "scope", defaultValue = "test", required = true)
    private String scope;

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

    @Override
    protected boolean doExecute(Logger output, ToolboxCommando toolboxCommando) throws Exception {
        ResolutionScope resolutionScope = ResolutionScope.parse(scope);
        toolboxCommando.versions(
                "managed dependencies",
                () ->
                        projectManagedDependenciesAsResolutionRoots(
                                        resolutionScope, toolboxCommando.parseDependencyMatcherSpec(depSpec))
                                .stream()
                                .map(ResolutionRoot::getArtifact),
                toolboxCommando.parseArtifactVersionMatcherSpec(artifactVersionMatcherSpec),
                output);
        toolboxCommando.versions(
                "dependencies",
                () ->
                        projectDependenciesAsResolutionRoots(
                                        resolutionScope, toolboxCommando.parseDependencyMatcherSpec(depSpec))
                                .stream()
                                .map(ResolutionRoot::getArtifact),
                toolboxCommando.parseArtifactVersionMatcherSpec(artifactVersionMatcherSpec),
                output);
        return true;
    }
}
