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
import eu.maveniverse.maven.toolbox.shared.internal.PomSuppliers;
import eu.maveniverse.maven.toolbox.shared.internal.jdom.JDomDocumentIO;
import eu.maveniverse.maven.toolbox.shared.internal.jdom.JDomPomEditor;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.artifact.Artifact;
import picocli.CommandLine;

/**
 * Creates a new project.
 */
@CommandLine.Command(name = "new-project", description = "Creates a new project")
@Mojo(name = "new-project", requiresProject = false, threadSafe = true)
public class NewProject extends HelloMojoSupport {
    /**
     * The project GAV.
     */
    @CommandLine.Parameters(index = "0", description = "The project GAV", arity = "1")
    @Parameter(property = "gav", required = true)
    private String gav;

    /**
     * Packaging.
     */
    @CommandLine.Option(
            names = {"--packaging"},
            defaultValue = "jar",
            description = "Packaging")
    @Parameter(property = "packaging", defaultValue = "jar", required = true)
    private String packaging;

    /**
     * Parent.
     */
    @CommandLine.Option(
            names = {"--parent"},
            description = "Parent")
    @Parameter(property = "parent")
    private String parent;

    /**
     * Force overwrite of existing POM file.
     */
    @CommandLine.Option(
            names = {"--force"},
            description = "Force overwrite of existing POM file")
    @Parameter(property = "force")
    private boolean force;

    @Override
    protected Result<Boolean> doExecute() throws Exception {
        Path pomFile = Path.of("pom.xml").toAbsolutePath();
        if (!force && Files.exists(pomFile)) {
            throw new IllegalStateException("pom.xml already exists in this directory; use --force to overwrite it");
        }
        Artifact projectArtifact = toProjectArtifact(gav);
        Artifact parentArtifact = toParentArtifact(parent);
        try (ToolboxCommando.EditSession editSession = getToolboxCommando().createEditSession(pomFile)) {
            editSession.edit(p -> {
                Files.writeString(
                        p,
                        PomSuppliers.empty400(
                                projectArtifact.getGroupId(),
                                projectArtifact.getArtifactId(),
                                projectArtifact.getVersion()));
                try (JDomDocumentIO documentIO = new JDomDocumentIO(p)) {
                    if (!"jar".equals(packaging)) {
                        JDomPomEditor.setPackaging(documentIO.getDocument().getRootElement(), packaging);
                    }
                    if (parentArtifact != null) {
                        JDomPomEditor.setParent(documentIO.getDocument().getRootElement(), parentArtifact);
                    }
                }
            });
        }

        return Result.success(Boolean.TRUE);
    }
}
