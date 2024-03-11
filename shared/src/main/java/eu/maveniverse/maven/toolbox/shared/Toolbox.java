/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

import java.util.List;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

public interface Toolbox {

    /**
     * Collects given, maybe even non-existent, {@link Artifact} with all the specified dependencies, managed
     * dependencies for given resolution scope.
     */
    CollectResult collect(
            ResolutionScope resolutionScope,
            Artifact root,
            List<Dependency> dependencies,
            List<Dependency> managedDependencies,
            List<RemoteRepository> remoteRepositories,
            boolean verbose)
            throws DependencyCollectionException;

    /**
     * Collects given existing {@link Dependency} by reusing POM information for given resolution scope.
     */
    CollectResult collect(
            ResolutionScope resolutionScope,
            Dependency root,
            List<RemoteRepository> remoteRepositories,
            boolean verbose)
            throws DependencyCollectionException;

    /**
     * Resolves given artifacts from given remote repositories.
     */
    List<ArtifactResult> resolveArtifacts(List<Artifact> artifacts, List<RemoteRepository> remoteRepositories)
            throws ArtifactResolutionException;
}
