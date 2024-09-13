/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.mp;

import eu.maveniverse.maven.toolbox.plugin.MPPluginMojoSupport;
import eu.maveniverse.maven.toolbox.shared.ResolutionRoot;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.artifact.Artifact;
import org.slf4j.Logger;

/**
 * Resolves transitively given project build plugin.
 */
@Mojo(name = "plugin-resolve", threadSafe = true)
public class PluginResolveMojo extends MPPluginMojoSupport {
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
    protected boolean doExecute(Logger output, ToolboxCommando toolboxCommando) throws Exception {
        Collection<Artifact> roots;
        ResolutionRoot root = pluginAsResolutionRoot(toolboxCommando, false);
        if (root != null) {
            roots = Collections.singleton(root.getArtifact());
        } else {
            roots = allProjectPluginsAsResolutionRoots(toolboxCommando).stream()
                    .map(ResolutionRoot::getArtifact)
                    .collect(Collectors.toList());
        }
        return toolboxCommando.resolve(
                roots, sources, javadoc, signature, toolboxCommando.artifactSink(sinkSpec), output);
    }
}
