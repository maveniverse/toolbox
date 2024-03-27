/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.mp;

import eu.maveniverse.maven.toolbox.plugin.MPPluginMojoSupport;
import eu.maveniverse.maven.toolbox.shared.ArtifactSinks;
import eu.maveniverse.maven.toolbox.shared.Output;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import java.util.Collections;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Resolves transitively given project build plugin.
 */
@Mojo(name = "plugin-resolve", requiresProject = false, threadSafe = true)
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

    @Override
    protected boolean doExecute(Output output, ToolboxCommando toolboxCommando) throws Exception {
        return toolboxCommando.resolve(
                Collections.singleton(pluginAsResolutionRoot(toolboxCommando).getArtifact()),
                sources,
                javadoc,
                signature,
                ArtifactSinks.nullArtifactSink(),
                output);
    }
}
