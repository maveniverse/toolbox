/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

import eu.maveniverse.maven.toolbox.shared.output.Output;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;

public interface ToolboxGraph {
    /**
     * Creates instance of {@link ToolboxGraph}, if it can. Graphviz is optional dependency.
     */
    static Optional<ToolboxGraph> create(Output output) {
        try {
            return Optional.of(new eu.maveniverse.maven.toolbox.shared.internal.ToolboxGraphImpl(output));
        } catch (LinkageError e) {
            return Optional.empty();
        }
    }

    /**
     * Returns the project module dependency graph of given root. As a side effect it also writes out rendered image.
     */
    Result<Map<ReactorLocator.ReactorProject, Collection<Dependency>>> projectDependencyGraph(
            ReactorLocator reactorLocator,
            boolean showExternal,
            ArtifactMatcher excludeSubprojectsMatcher,
            DependencyMatcher excludeDependencyMatcher,
            Path output)
            throws IOException;

    Map<ReactorLocator.ReactorProject, Collection<Dependency>> projectDependencyGraph(
            ReactorLocator reactorLocator,
            boolean showExternal,
            ArtifactMatcher excludeSubprojectsMatcher,
            DependencyMatcher excludeDependencyMatcher);

    Map<Artifact, String> labels(Map<ReactorLocator.ReactorProject, Collection<Dependency>> graph);
}
