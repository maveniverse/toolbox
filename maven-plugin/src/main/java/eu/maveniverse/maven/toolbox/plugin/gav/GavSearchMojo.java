/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.gav;

import eu.maveniverse.maven.toolbox.plugin.GavSearchMojoSupport;
import eu.maveniverse.maven.toolbox.shared.Output;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import java.io.IOException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Searches artifacts using SMO service.
 */
@Mojo(name = "gav-search", requiresProject = false, threadSafe = true)
public class GavSearchMojo extends GavSearchMojoSupport {
    /**
     * The "expression" for search.
     */
    @Parameter(property = "expression", required = true)
    private String expression;

    @Override
    protected boolean doExecute(Output output, ToolboxCommando toolboxCommando) throws IOException {
        return toolboxCommando.search(getRemoteRepository(toolboxCommando), expression, output);
    }
}
