/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.mp;

import eu.maveniverse.maven.toolbox.plugin.MPMojoSupport;
import eu.maveniverse.maven.toolbox.shared.ReactorLocator;
import eu.maveniverse.maven.toolbox.shared.Result;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import java.io.File;
import java.util.Collection;
import java.util.Map;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.graph.Dependency;

/**
 * Renders project interdependencies of Maven Projects as image.
 */
@Mojo(name = "project-dependency-graph", aggregator = true, threadSafe = true)
public class ProjectDependencyGraphMojo extends MPMojoSupport {

    /**
     * Set it {@code true} to include external direct dependencies as well.
     */
    @Parameter(property = "showExternal", defaultValue = "false", required = true)
    private boolean showExternal;

    /**
     * The exclusion dependency matcher (inverted!) to apply to graph. Default is {@code none()}.
     */
    @Parameter(property = "excludeDependencyMatcherSpec", defaultValue = "none()", required = true)
    private String excludeDependencyMatcherSpec;

    /**
     * The exclusion artifact matcher (inverted!) to apply to reactor modules. Default is {@code none()}.
     */
    @Parameter(property = "excludeSubprojectsMatcherSpec", defaultValue = "none()", required = true)
    private String excludeSubprojectsMatcherSpec;

    /**
     * Set the project selector, like {@code -rf} Maven command uses it, can be {@code :A} or {@code G:A}. If the
     * selector is set, it must match exactly one project within reactor, otherwise it will fail. By default,
     * selector is {@code null}, and no selected project will be set.
     */
    @Parameter(property = "selector")
    private String selector;

    /**
     * The location to write resulting image to.
     */
    @Parameter(
            property = "output",
            defaultValue = "${project.build.directory}/project-dependency-graph.svg",
            required = true)
    private File output;

    @Override
    protected Result<Map<ReactorLocator.ReactorProject, Collection<Dependency>>> doExecute() throws Exception {
        ToolboxCommando commando = getToolboxCommando();
        return commando.projectDependencyGraph(
                getReactorLocator(selector),
                showExternal,
                commando.parseArtifactMatcherSpec(excludeSubprojectsMatcherSpec),
                commando.parseDependencyMatcherSpec(excludeDependencyMatcherSpec),
                output.toPath());
    }
}
