/*
 * Copyright (c) 2023-2026 Maveniverse Org.
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
 * Shows effective model for project; is aggregator mojo.
 */
@Mojo(name = "effective-model", aggregator = true, threadSafe = true)
public class EffectiveModelAggregatorMojo extends MPMojoSupport {
    /**
     * Set the project selector, like {@code -rf} Maven command uses it, can be {@code :A} or {@code G:A}. If the
     * selector is set, it must match exactly one project within reactor, otherwise it will fail. By default,
     * selector is {@code null}, and Maven session "current project" is used.
     */
    @Parameter(property = "selector")
    private String selector;

    @Override
    protected Result<Model> doExecute() throws Exception {
        ToolboxCommando toolboxCommando = getToolboxCommando();
        return toolboxCommando.effectiveModel(getReactorLocator(selector));
    }
}
