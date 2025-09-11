/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

import java.util.Collection;
import java.util.Map;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;

public interface ToolboxGraph {
    Map<ReactorLocator.ReactorProject, Collection<Dependency>> projectDependencyGraph(
            ReactorLocator reactorLocator,
            boolean showExternal,
            ArtifactMatcher excludeSubprojectsMatcher,
            DependencyMatcher excludeDependencyMatcher);

    Map<Artifact, String> labels(Map<ReactorLocator.ReactorProject, Collection<Dependency>> graph);
}
