/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static eu.maveniverse.maven.toolbox.shared.internal.ToolboxCommandoImpl.source;
import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.toolbox.shared.ArtifactMatcher;
import eu.maveniverse.maven.toolbox.shared.DependencyMatcher;
import eu.maveniverse.maven.toolbox.shared.ProjectLocator;
import eu.maveniverse.maven.toolbox.shared.ReactorLocator;
import eu.maveniverse.maven.toolbox.shared.StringUtils;
import eu.maveniverse.maven.toolbox.shared.ToolboxGraph;
import eu.maveniverse.maven.toolbox.shared.output.Output;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.aether.artifact.Artifact;
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

    @Override
    public Map<Artifact, String> labels(Map<ReactorLocator.ReactorProject, Collection<Dependency>> graph) {
        Set<String> reactorVersions =
                graph.keySet().stream().map(p -> p.artifact().getVersion()).collect(Collectors.toSet());
        Set<String> reactorGroupIds =
                graph.keySet().stream().map(p -> p.artifact().getGroupId()).collect(Collectors.toSet());

        HashMap<String, String> reactorGroupIdMapping = new HashMap<>();
        if (reactorGroupIds.size() == 1) {
            reactorGroupIdMapping.put(reactorGroupIds.iterator().next(), "");
        } else {
            String commonPrefix = commonPrefix(reactorGroupIds);
            String shortPrefix = shortPrefix(commonPrefix);
            reactorGroupIds.forEach(g -> reactorGroupIdMapping.put(g, formatLabel(commonPrefix, shortPrefix, g)));
        }

        HashMap<Artifact, String> result = new HashMap<>();
        Stream.concat(
                        graph.keySet().stream().map(ProjectLocator.Project::artifact),
                        graph.values().stream().flatMap(Collection::stream).map(Dependency::getArtifact))
                .forEach(a -> {
                    String source = a.getProperty("source", "internal");
                    if ("internal".equals(source)) {
                        if (reactorVersions.size() == 1) {
                            // omit V
                            result.putIfAbsent(a, reactorGroupIdMapping.get(a.getGroupId()) + ":" + a.getArtifactId());
                        } else {
                            // all
                            result.putIfAbsent(
                                    a,
                                    reactorGroupIdMapping.get(a.getGroupId()) + ":" + a.getArtifactId() + ":"
                                            + a.getVersion());
                        }
                    } else {
                        result.putIfAbsent(a, a.getGroupId() + ":" + a.getArtifactId() + ":" + a.getVersion());
                    }
                });
        return result;
    }

    protected String commonPrefix(Collection<String> groupIds) {
        return StringUtils.getCommonPrefix(groupIds.toArray(new String[0]));
    }

    protected String shortPrefix(String commonPrefix) {
        if (!commonPrefix.contains(".")) {
            return commonPrefix;
        }
        return Arrays.stream(commonPrefix.split("\\."))
                .map(s -> s.substring(0, 1))
                .collect(Collectors.joining("."));
    }

    protected String formatLabel(String commonPrefix, String shortPrefix, String value) {
        return shortPrefix + value.substring(commonPrefix.length());
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
                    result.computeIfAbsent(project, p -> new HashSet<>())
                            .add(dependency.setArtifact(dependency.getArtifact()));
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
                    result.computeIfAbsent(project, p -> new HashSet<>())
                            .add(dependency.setArtifact(source(dependency.getArtifact(), true)));
                }
            }
        }
    }
}
