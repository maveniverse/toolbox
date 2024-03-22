/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.gav;

import eu.maveniverse.maven.toolbox.plugin.GavMojoSupport;
import eu.maveniverse.maven.toolbox.shared.ResolutionScope;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Resolves transitively a given GAV and outputs used repositories.
 */
@Mojo(name = "gav-list-repositories", threadSafe = true)
public final class GavListRepositoriesMojo extends GavMojoSupport {
    /**
     * The resolution scope to display, accepted values are "runtime", "compile", "test", etc.
     */
    @Parameter(property = "scope", defaultValue = "runtime", required = true)
    private String scope;

    /**
     * The artifact coordinates in the format {@code <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>}
     * to display tree for.
     */
    @Parameter(property = "gav", required = true)
    private String gav;

    /**
     * Apply BOMs, if needed. Comma separated GAVs.
     */
    @Parameter(property = "boms", defaultValue = "")
    private String boms;

    @Override
    protected void doExecute(ToolboxCommando toolboxCommando) throws Exception {
        toolboxCommando.listRepositories(ResolutionScope.parse(scope), toolboxCommando.loadGav(gav, csv(boms)), output);
    }
}
