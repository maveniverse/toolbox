/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.mp;

import eu.maveniverse.maven.toolbox.plugin.MPMojoSupport;
import eu.maveniverse.maven.toolbox.shared.ResolutionScope;
import eu.maveniverse.maven.toolbox.shared.Result;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import java.util.List;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.artifact.Artifact;
import org.slf4j.Logger;

/**
 * Resolves transitively selected dependencies.
 */
@Mojo(name = "resolve-transitive", threadSafe = true)
public class ResolveTransitiveMojo extends MPMojoSupport {
    /**
     * The resolution scope to resolve, accepted values are "runtime", "compile", "test", etc.
     */
    @Parameter(property = "scope", defaultValue = "runtime", required = true)
    private String scope;

    /**
     * The dependency matcher spec.
     */
    @Parameter(property = "depSpec", defaultValue = "any()")
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
     * The artifact sink spec (default: "null()").
     */
    @Parameter(property = "sinkSpec", defaultValue = "null()", required = true)
    private String sinkSpec;

    @Override
    protected Result<List<Artifact>> doExecute(Logger output, ToolboxCommando toolboxCommando) throws Exception {
        ResolutionScope resolutionScope = ResolutionScope.parse(scope);
        return toolboxCommando.resolveTransitive(
                resolutionScope,
                projectDependenciesAsResolutionRoots(
                        resolutionScope, toolboxCommando.parseDependencyMatcherSpec(depSpec)),
                sources,
                javadoc,
                signature,
                toolboxCommando.artifactSink(sinkSpec),
                output);
    }
}
