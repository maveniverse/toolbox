/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.mp;

import eu.maveniverse.domtrip.maven.PomEditor;
import eu.maveniverse.maven.toolbox.plugin.MPMojoSupport;
import eu.maveniverse.maven.toolbox.shared.Result;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import java.util.ArrayList;
import java.util.function.Consumer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Sets version of current project and all children.
 */
@Mojo(name = "set-version", aggregator = true, threadSafe = true)
public class SetVersionMojo extends MPMojoSupport {
    /**
     * The new version.
     */
    @Parameter(property = "version", required = true)
    private String version;

    /**
     * Optional; comma separated properties, that if found, should be set also to new version.
     */
    @Parameter(property = "properties")
    private String properties;

    @Override
    protected Result<Boolean> doExecute() throws Exception {
        ToolboxCommando toolboxCommando = getToolboxCommando();

        ArrayList<Consumer<PomEditor>> transformers = new ArrayList<>();
        transformers.add(s -> s.setVersion(version));
        if (properties != null) {
            for (String property : properties.split(",")) {
                if (!property.trim().isEmpty()) {
                    transformers.add(s -> s.properties().updateProperty(false, property, version));
                }
            }
        }

        Result<Boolean> result = Result.success(true);
        for (MavenProject project : mavenSession.getProjects()) {
            try (ToolboxCommando.EditSession editSession =
                    toolboxCommando.createEditSession(project.getFile().toPath())) {
                result = toolboxCommando.editPom(editSession, transformers);
            }
            if (!result.isSuccess()) {
                throw new MojoExecutionException("Failed to update version of " + project.getArtifactId());
            }
        }
        return result;
    }
}
