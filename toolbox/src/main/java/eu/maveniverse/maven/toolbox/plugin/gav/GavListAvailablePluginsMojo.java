/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.gav;

import eu.maveniverse.maven.toolbox.plugin.GavMojoSupport;
import eu.maveniverse.maven.toolbox.shared.Result;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import java.io.File;
import java.util.List;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.artifact.Artifact;
import picocli.CommandLine;

/**
 * List available plugins for given Gs.
 */
@CommandLine.Command(name = "list-available-plugins", description = "List available plugins for given Gs")
@Mojo(name = "gav-list-available-plugins", requiresProject = false, threadSafe = true)
public class GavListAvailablePluginsMojo extends GavMojoSupport {
    /**
     * The comma separated GroupIDs to list.
     */
    @CommandLine.Parameters(
            index = "0",
            defaultValue = "org.apache.maven.plugins,org.codehaus.mojo",
            description = "The comma separated GroupIDs to list",
            arity = "1")
    @Parameter(property = "groupIds")
    private String groupIds;

    /**
     * Generate POM w/ pluginManagement out of results.
     */
    @CommandLine.Option(
            names = {"--toPom"},
            description = "Output it into given POM")
    @Parameter(property = "toPom")
    private File toPom;

    /**
     * Whether to update existing only, or upsert.
     */
    @CommandLine.Option(
            names = {"--upsert"},
            description = "Whether to update existing only, or upsert")
    @Parameter(property = "upsert")
    private boolean upsert;

    @Override
    protected Result<List<Artifact>> doExecute() throws Exception {
        if (groupIds == null || groupIds.trim().isEmpty()) {
            groupIds = String.join(",", mojoSettings.getPluginGroups());
        }
        Result<List<Artifact>> result = getToolboxCommando().listAvailablePlugins(csv(groupIds));
        if (result.isSuccess() && toPom != null) {
            try (ToolboxCommando.EditSession es = getToolboxCommando().createEditSession(toPom.toPath())) {
                return getToolboxCommando()
                        .editPom(
                                es,
                                ToolboxCommando.PomOpSubject.MANAGED_PLUGINS,
                                upsert ? ToolboxCommando.Op.UPSERT : ToolboxCommando.Op.UPDATE,
                                () -> result.getData().orElseThrow().stream());
            }
        }
        return result;
    }
}
