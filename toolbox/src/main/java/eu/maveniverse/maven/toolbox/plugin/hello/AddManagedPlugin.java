/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.hello;

import eu.maveniverse.maven.toolbox.shared.Result;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import eu.maveniverse.maven.toolbox.shared.internal.domtrip.DOMTripUtils;
import java.util.Collections;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.artifact.Artifact;
import picocli.CommandLine;

/**
 * Adds managed plugin.
 */
@CommandLine.Command(name = "add-managed-plugin", description = "Adds managed plugin")
@Mojo(name = "add-managed-plugin", requiresProject = false, threadSafe = true)
public class AddManagedPlugin extends HelloProjectMojoSupport {
    /**
     * The plugin GAV.
     */
    @CommandLine.Parameters(index = "0", description = "The plugin GAV", arity = "1")
    @Parameter(property = "gav", required = true)
    private String gav;

    @Override
    protected Result<Boolean> doExecute() throws Exception {
        Artifact plugin = toPluginArtifact(gav);
        try (ToolboxCommando.EditSession editSession = getToolboxCommando().createEditSession(getRootPom())) {
            getToolboxCommando().editPom(editSession, Collections.singletonList(s -> s.plugins()
                    .updateManagedPlugin(true, DOMTripUtils.toDomTrip(plugin))));
        }
        return Result.success(Boolean.TRUE);
    }
}
