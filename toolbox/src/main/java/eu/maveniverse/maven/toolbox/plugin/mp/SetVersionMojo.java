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
import eu.maveniverse.maven.toolbox.shared.internal.jdom.JDomPomEditor;
import eu.maveniverse.maven.toolbox.shared.internal.jdom.JDomPomTransformer;
import java.util.ArrayList;
import java.util.function.Consumer;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Sets version of current project and all children.
 */
@Mojo(name = "set-version", threadSafe = true)
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
        Result<Boolean> result;

        ArrayList<Consumer<JDomPomTransformer.TransformationContext>> transformers = new ArrayList<>();
        transformers.add(c -> JDomPomEditor.setVersion(c.getDocument().getRootElement(), version));
        if (properties != null) {
            for (String property : properties.split(",")) {
                if (!property.trim().isEmpty()) {
                    transformers.add(
                            c -> JDomPomEditor.setProperty(c.getDocument().getRootElement(), property, version, false));
                }
            }
        }

        try (ToolboxCommando.EditSession editSession =
                toolboxCommando.createEditSession(mavenProject.getFile().toPath())) {
            result = toolboxCommando.doEdit(editSession, transformers);
        }
        return result;
    }
}
