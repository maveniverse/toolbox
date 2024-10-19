/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.mp;

import eu.maveniverse.maven.toolbox.plugin.MPMojoSupport;
import eu.maveniverse.maven.toolbox.shared.Result;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import java.util.List;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.artifact.Artifact;

/**
 * Resolves selected dependencies and copies resulting artifacts to target.
 */
@Mojo(name = "copy-dependencies", threadSafe = true)
public final class CopyDependenciesMojo extends MPMojoSupport {

    /**
     * The artifact sink spec (default: "null()").
     */
    @Parameter(property = "sinkSpec", defaultValue = "null()", required = true)
    private String sinkSpec;

    /**
     * The dependency matcher spec.
     */
    @Parameter(property = "depSpec", defaultValue = "any()")
    private String depSpec;

    @Override
    protected Result<List<Artifact>> doExecute() throws Exception {
        ToolboxCommando toolboxCommando = getToolboxCommando();
        return toolboxCommando.copy(
                () -> projectAsResolutionRoot().getDependencies().stream()
                        .filter(toolboxCommando.parseDependencyMatcherSpec(depSpec))
                        .map(toolboxCommando::toArtifact),
                toolboxCommando.artifactSink(sinkSpec));
    }
}
