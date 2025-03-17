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
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.artifact.Artifact;
import picocli.CommandLine;

/**
 * Creates a new subproject.
 */
@CommandLine.Command(name = "new-subproject", description = "Creates a new subproject")
@Mojo(name = "new-subproject", requiresProject = false, threadSafe = true)
public class NewSubProject extends HelloProjectMojoSupport {
    /**
     * The subproject GAV.
     */
    @CommandLine.Parameters(index = "0", description = "The subproject GAV", arity = "1")
    @Parameter(property = "gav", required = true)
    private String gav;

    /**
     * Packaging.
     */
    @CommandLine.Option(
            names = {"--packaging"},
            description = "Packaging",
            defaultValue = "jar")
    @Parameter(property = "packaging", defaultValue = "jar")
    private String packaging;

    @Override
    protected Result<Boolean> doExecute() throws Exception {
        Artifact subProjectArtifact = toSubProjectArtifact(gav);
        if (!force && Files.exists(subProjectArtifact.getFile().toPath())) {
            throw new IllegalStateException("pom.xml already exists in this directory; use --force to overwrite it");
        }
        Files.createDirectories(subProjectArtifact.getFile().toPath().getParent());
        try (ToolboxCommando.EditSession editSession = getToolboxCommando()
                .createEditSession(subProjectArtifact.getFile().toPath())) {
            editSession.edit(p -> {
                Files.writeString(
                        p,
                        PomSuppliers.empty400(
                                subProjectArtifact.getGroupId(),
                                subProjectArtifact.getArtifactId(),
                                subProjectArtifact.getVersion()));
                try (JDomDocumentIO documentIO = new JDomDocumentIO(p)) {
                    String effectivePackaging = "jar";
                    if (packaging != null) {
                        effectivePackaging = packaging;
                    } else if (subProjectArtifact.getGroupId().endsWith("." + subProjectArtifact.getArtifactId())) {
                        effectivePackaging = "pom";
                    }
                    if (!"jar".equals(effectivePackaging)) {
                        JDomPomEditor.setPackaging(documentIO.getDocument().getRootElement(), effectivePackaging);
                    }
                    JDomPomEditor.setParent(documentIO.getDocument().getRootElement(), getCurrentProjectArtifact());
                }
            });
        }
        try (ToolboxCommando.EditSession editSession = getToolboxCommando().createEditSession(getRootPom())) {
            editSession.edit(p -> {
                try (JDomDocumentIO documentIO = new JDomDocumentIO(p)) {
                    JDomPomEditor.addSubProject(
                            documentIO.getDocument().getRootElement(), subProjectArtifact.getArtifactId());
                }
            });
        }
        return Result.success(Boolean.TRUE);
    }
}
