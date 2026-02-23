/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.hello;

import eu.maveniverse.maven.toolbox.shared.Result;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.artifact.Artifact;
import picocli.CommandLine;

/**
 * Adds core extension.
 */
@CommandLine.Command(name = "add-core-extension", description = "Adds core extension")
@Mojo(name = "add-core-extension", requiresProject = false, threadSafe = true)
public class AddCoreExtension extends HelloProjectMojoSupport {
    /**
     * The extension GAV.
     */
    @CommandLine.Parameters(index = "0", description = "The extension GAV", arity = "1")
    @Parameter(property = "gav", required = true)
    private String gav;

    /**
     * The scope where to look.
     */
    @CommandLine.Option(
            names = {"scope"},
            defaultValue = "project",
            description = "The scope where to look.")
    @Parameter(property = "scope", defaultValue = "project")
    private String scope;

    @Override
    protected Result<Boolean> doExecute() throws Exception {
        ToolboxCommando toolboxCommando = getToolboxCommando();
        Artifact extension = toCoreExtensionArtifact(gav);
        ToolboxCommando.ExtensionsScope extensionsScope =
                ToolboxCommando.ExtensionsScope.valueOf(scope.toUpperCase(Locale.ROOT));
        Path extensionsXml;
        if (extensionsScope == ToolboxCommando.ExtensionsScope.PROJECT) {
            extensionsXml = getRootPom().getParent().resolve(".mvn").resolve("extensions.xml");
        } else {
            extensionsXml = toolboxCommando.extensionsPath(extensionsScope);
        }

        try (ToolboxCommando.EditSession editSession = getToolboxCommando().createEditSession(extensionsXml)) {
            toolboxCommando.editExtensions(editSession, ToolboxCommando.Op.UPSERT, List.of(extension)::stream);
        }
        return Result.success(Boolean.TRUE);
    }
}
