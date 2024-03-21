/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.gav;

import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import java.io.IOException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "gav-list", requiresProject = false, threadSafe = true)
public class GavListMojo extends GavSearchMojoSupport {
    /**
     * The "gavoid" part to list.
     */
    @Parameter(property = "gavoid", required = true)
    private String gavoid;

    @Override
    protected void doExecute(ToolboxCommando toolboxCommando) throws MojoExecutionException {
        try {
            toolboxCommando.list(getRemoteRepository(toolboxCommando), gavoid, output);
        } catch (IOException e) {
            throw new MojoExecutionException(e);
        }
    }
}
