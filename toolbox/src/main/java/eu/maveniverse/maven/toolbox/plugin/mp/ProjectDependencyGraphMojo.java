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
import java.util.Collection;
import java.util.Map;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.graph.Dependency;

/**
 * Displays project interdependencies of Maven Projects.
 */
@Mojo(name = "project-dependency-graph", aggregator = true, threadSafe = true)
public class ProjectDependencyGraphMojo extends MPMojoSupport {

    /**
     * Set it {@code true} to include external direct dependencies as well.
     */
    @Parameter(property = "showExternal", defaultValue = "false", required = true)
    private boolean showExternal;

    /**
     * Set the project selector, like {@code -rf} Maven command uses it, can be {@code :A} or {@code G:A}. If the
     * selector is set, it must match exactly one project within reactor, otherwise it will fail. By default,
     * selector is {@code null}, and no selected project will be set.
     */
    @Parameter(property = "selector")
    private String selector;

    @Override
    protected Result<Map<ReactorLocator.ReactorProject, Collection<Dependency>>> doExecute() throws Exception {
        ReactorLocator locator = getReactorLocator(selector);
        Result<Map<ReactorLocator.ReactorProject, Collection<Dependency>>> result =
                getToolboxCommando().projectDependencyGraph(locator, showExternal);
        if (result.isSuccess()) {
            Map<ReactorLocator.ReactorProject, Collection<Dependency>> map =
                    result.getData().orElseThrow();
            // TODO:
            // common G prefix
            // versions (if all reactor same; omit)
            // dep filters
            // scope filters
            System.out.println("digraph Reactor {");
            for (Map.Entry<ReactorLocator.ReactorProject, Collection<Dependency>> entry : map.entrySet()) {
                String left = entry.getKey().artifact().getArtifactId();
                for (Dependency dependency : entry.getValue()) {
                    String right = dependency.getArtifact().toString();
                    if (locator.locateProject(dependency.getArtifact()).isPresent()) {
                        right = dependency.getArtifact().getArtifactId();
                    }
                    System.out.printf("\"%s\" -> \"%s\"%n", left, right);
                }
            }
            System.out.println("}");
        }
        return result;
    }
}
