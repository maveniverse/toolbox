/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.mp;

import eu.maveniverse.maven.toolbox.plugin.MPMojoSupport;
import eu.maveniverse.maven.toolbox.shared.Result;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Objects;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

/**
 * Flattens a BOM and attaches it.
 */
@Mojo(name = "flatten-bom", threadSafe = true)
public class FlattenBomMojo extends MPMojoSupport {
    /**
     * The GAV to emit flattened BOM with.
     */
    @Parameter(property = "gav", defaultValue = "${project.groupId}:${project.artifactId}:${project.version}")
    private String gav;

    /**
     * The GAV of BOM to flatten.
     */
    @Parameter(property = "bom", required = true)
    private String bom;

    /**
     * The output file.
     */
    @Parameter(property = "outputFile", defaultValue = "${project.build.directory}/flatten-bom.xml", required = true)
    private File outputFile;

    @Override
    protected Result<Model> doExecute() throws Exception {
        ToolboxCommando toolboxCommando = getToolboxCommando();
        Artifact output = new DefaultArtifact(gav);
        Artifact input = new DefaultArtifact(bom);
        Result<Model> result;
        if (isReactorProject(input)) {
            result = toolboxCommando.flattenBOM(
                    output,
                    getReactorLocator(input.getGroupId() + ":" + input.getArtifactId() + ":" + input.getVersion()));
        } else {
            result = toolboxCommando.flattenBOM(output, toolboxCommando.loadGav(bom));
        }
        if (result.isSuccess()) {
            Model model = result.getData().orElseThrow();
            try (OutputStream out = Files.newOutputStream(outputFile.toPath())) {
                new MavenXpp3Writer().write(out, model);
            }
            if (!output.getClassifier().trim().isEmpty()) {
                getLog().debug("Attaching BOM w/ classifier: " + output.getClassifier());
                org.apache.maven.artifact.DefaultArtifact artifact = new org.apache.maven.artifact.DefaultArtifact(
                        output.getGroupId(),
                        output.getArtifactId(),
                        output.getVersion(),
                        null,
                        "pom",
                        output.getClassifier(),
                        artifactHandlerManager.getArtifactHandler("pom"));
                artifact.setFile(outputFile);
                mavenProject.addAttachedArtifact(artifact);
            } else if (Objects.equals("pom", mavenProject.getPackaging())
                    && mavenProject.getModules().isEmpty()) {
                getLog().debug("Replacing module POM w/ generated BOM");
                mavenProject.setFile(outputFile);
            } else {
                throw new MojoExecutionException(
                        "Cannot replace project POM: invalid project (packaging=pom w/o modules)");
            }
        }
        return result;
    }
}
