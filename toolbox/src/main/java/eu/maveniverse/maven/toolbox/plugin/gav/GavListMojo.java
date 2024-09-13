/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.gav;

import eu.maveniverse.maven.toolbox.plugin.GavSearchMojoSupport;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import picocli.CommandLine;

/**
 * Lists remote repository by given "gavoid" (G or G:A or G:A:V where V may be a version constraint).
 */
@CommandLine.Command(
        name = "list",
        description = "Lists remote repository by given 'gavoid' (G or G:A or G:A:V where V may be version constraint)")
@Mojo(name = "gav-list", requiresProject = false, threadSafe = true)
public class GavListMojo extends GavSearchMojoSupport {
    /**
     * The GAV-oid to list (G or G:A or G:A:V), where V may be a version constraint.
     */
    @CommandLine.Parameters(index = "0", description = "The GAV-oid to list (G or G:A or G:A:V)")
    @Parameter(property = "gavoid", required = true)
    private String gavoid;

    @Override
    protected boolean doExecute(Logger output, ToolboxCommando toolboxCommando) throws Exception {
        return toolboxCommando.list(getRemoteRepository(toolboxCommando), gavoid, null, output);
    }
}
