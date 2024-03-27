/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.mp;

import eu.maveniverse.maven.toolbox.plugin.MPMojoSupport;
import eu.maveniverse.maven.toolbox.shared.Output;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import java.util.stream.Collectors;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.graph.Dependency;

/**
 * Resolves selected dependencies.
 */
@Mojo(name = "resolve", requiresProject = false, threadSafe = true)
public class ResolveMojo extends MPMojoSupport {
    /**
     * The dependency matcher spec.
     */
    @Parameter(property = "depSpec", required = true)
    private String depSpec;

    /**
     * Resolve sources JAR as well (derive coordinates from GAV).
     */
    @Parameter(property = "sources", defaultValue = "false")
    private boolean sources;

    /**
     * Resolve javadoc JAR as well (derive coordinates from GAV).
     */
    @Parameter(property = "javadoc", defaultValue = "false")
    private boolean javadoc;

    /**
     * Resolve GnuPG signature as well (derive coordinates from GAV).
     */
    @Parameter(property = "signature", defaultValue = "false")
    private boolean signature;

    /**
     * The artifact sink spec (default: "null").
     */
    @Parameter(property = "sinkSpec", defaultValue = "null", required = true)
    private String sinkSpec;

    @Override
    protected boolean doExecute(Output output, ToolboxCommando toolboxCommando) throws Exception {
        return toolboxCommando.resolve(
                projectAsResolutionRoot().getDependencies().stream()
                        .filter(toolboxCommando.parseDependencyMatcherSpec(depSpec))
                        .map(Dependency::getArtifact)
                        .collect(Collectors.toList()),
                sources,
                javadoc,
                signature,
                toolboxCommando.artifactSink(output, sinkSpec),
                output);
    }
}
