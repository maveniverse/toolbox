/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.toolbox.shared.ArtifactMatcher;
import eu.maveniverse.maven.toolbox.shared.DependencyMatcher;
import eu.maveniverse.maven.toolbox.shared.ReactorLocator;
import eu.maveniverse.maven.toolbox.shared.ToolboxGraph;
import eu.maveniverse.maven.toolbox.shared.output.Output;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import org.eclipse.aether.graph.Dependency;

public class ToolboxGraphImpl implements ToolboxGraph {
    protected final Output output;

    public ToolboxGraphImpl(Output output) {
        this.output = requireNonNull(output, "output");
    }

    @Override
    public Map<ReactorLocator.ReactorProject, Collection<Dependency>> projectDependencyGraph(
            ReactorLocator reactorLocator,
            boolean showExternal,
            ArtifactMatcher excludeSubprojectsMatcher,
            DependencyMatcher excludeDependencyMatcher) {
        HashMap<ReactorLocator.ReactorProject, Collection<Dependency>> result = new HashMap<>();
        if (reactorLocator.getSelectedProject().isPresent()) {
            doProjectDependencyGraph(
                    result,
                    showExternal,
                    excludeSubprojectsMatcher,
                    excludeDependencyMatcher,
                    reactorLocator,
                    reactorLocator.getSelectedProject().orElseThrow());
        } else {
            for (ReactorLocator.ReactorProject project : reactorLocator.getAllProjects()) {
                if (!excludeSubprojectsMatcher.test(project.artifact())) {
                    doProjectDependencyGraph(
                            result,
                            showExternal,
                            excludeSubprojectsMatcher,
                            excludeDependencyMatcher,
                            reactorLocator,
                            project);
                }
            }
        }
        return result;
    }

    protected void doProjectDependencyGraph(
            HashMap<ReactorLocator.ReactorProject, Collection<Dependency>> result,
            boolean showExternal,
            ArtifactMatcher excludeSubprojectsMatcher,
            DependencyMatcher excludeDependencyMatcher,
            ReactorLocator reactorLocator,
            ReactorLocator.ReactorProject project) {
        for (Dependency dependency : project.dependencies()) {
            Optional<ReactorLocator.ReactorProject> rp = reactorLocator.locateProject(dependency.getArtifact());
            boolean isReactorMember = rp.isPresent();
            if (isReactorMember) {
                if (!excludeSubprojectsMatcher.test(dependency.getArtifact())
                        && !excludeDependencyMatcher.test(dependency)) {
                    result.computeIfAbsent(project, p -> new HashSet<>()).add(dependency);
                    doProjectDependencyGraph(
                            result,
                            showExternal,
                            excludeSubprojectsMatcher,
                            excludeDependencyMatcher,
                            reactorLocator,
                            rp.orElseThrow());
                }
            } else {
                if (showExternal && !excludeDependencyMatcher.test(dependency)) {
                    result.computeIfAbsent(project, p -> new HashSet<>()).add(dependency);
                }
            }
        }
    }
}
