/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.gav;

import eu.maveniverse.maven.toolbox.plugin.GavSearchMojoSupport;
import eu.maveniverse.maven.toolbox.shared.Result;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import java.util.List;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
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
    protected Result<List<String>> doExecute() throws Exception {
        ToolboxCommando toolboxCommando = getToolboxCommando();
        return toolboxCommando.list(getRemoteRepository(toolboxCommando), gavoid, null);
    }
}
