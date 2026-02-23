/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.hello;

import static eu.maveniverse.maven.toolbox.shared.internal.domtrip.DOMTripUtils.toDomTrip;

import eu.maveniverse.maven.toolbox.shared.Result;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import eu.maveniverse.maven.toolbox.shared.internal.PomSuppliers;
import java.nio.file.Files;
import java.util.Collections;
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
            description = "Packaging")
    @Parameter(property = "packaging")
    private String packaging;

    /**
     * Parent.
     */
    @CommandLine.Option(
            names = {"--parent"},
            description = "Parent")
    @Parameter(property = "parent")
    private String parent;

    @Override
    protected Result<Boolean> doExecute() throws Exception {
        if (!force && Files.exists(getRootPom())) {
            throw new IllegalStateException("pom.xml already exists in this directory; use --force to overwrite it");
        }
        Artifact projectArtifact = toRootProjectArtifact(gav);
        Artifact parentArtifact = toParentArtifact(parent);
        try (ToolboxCommando.EditSession editSession =
                getToolboxCommando().createEditSession(projectArtifact.getFile().toPath())) {
            // "reset" POM (or just one from scratch)
            editSession.edit(p -> {
                Files.writeString(
                        p,
                        PomSuppliers.empty400(
                                projectArtifact.getGroupId(),
                                projectArtifact.getArtifactId(),
                                projectArtifact.getVersion()));
            });
            // apply changes
            getToolboxCommando().editPom(editSession, Collections.singletonList(s -> {
                String effectivePackaging = "jar";
                if (packaging != null) {
                    effectivePackaging = packaging;
                } else if (projectArtifact.getGroupId().endsWith("." + projectArtifact.getArtifactId())) {
                    effectivePackaging = "pom";
                }
                s.setPackaging(effectivePackaging);
                if (parentArtifact != null) {
                    s.parent().setParent(toDomTrip(parentArtifact));
                }
            }));
        }

        return Result.success(Boolean.TRUE);
    }
}
