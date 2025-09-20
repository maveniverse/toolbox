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
import org.apache.maven.model.Model;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Shows effective model for project.
 */
@Mojo(name = "effective-model-all", threadSafe = true)
public class EffectiveModelMojo extends MPMojoSupport {

    /**
     * Whether to output verbose model or not.
     */
    @Parameter(property = "verbose", defaultValue = "false", required = true)
    private boolean verbose;

    @Override
    protected Result<Model> doExecute() throws Exception {
        ToolboxCommando toolboxCommando = getToolboxCommando();
        Result<Model> result = toolboxCommando.effectiveModel(getReactorLocator(null));
        if (result.isSuccess()) {
            Model model = result.getData().orElseThrow();
            Result<String> modelString = toolboxCommando.modelToString(model, verbose);
            if (modelString.isSuccess()) {
                getOutput().tell(modelString.getData().orElseThrow());
            }
        }
        return result;
    }
}
