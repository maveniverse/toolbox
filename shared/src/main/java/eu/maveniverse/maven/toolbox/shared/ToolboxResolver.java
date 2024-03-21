/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.version.Version;

/**
 * The Toolbox resolver implements "lower level" Resolver related operations (like fixed collection).
 */
public interface ToolboxResolver {
    /**
     * Loads up (and resolves if needed) artifact descriptor of given artifact.
     */
    ArtifactDescriptorResult readArtifactDescriptor(Artifact artifact) throws ArtifactDescriptorException;

    /**
     * Imports BOMs (specified as GAV strings).
     */
    List<Dependency> importBOMs(Collection<String> boms) throws ArtifactDescriptorException;

    /**
     * Parses GAV string (maybe incomplete, without version) into {@link Artifact}. May use manage dependencies to do so.
     */
    Artifact parseGav(String gav, List<Dependency> managedDependencies);

    /**
     * Parses remote repository string into {@link RemoteRepository}. It may be {@code url} only, or {@code id::url} form.
     * In former case, repository ID will be "mima".
     */
    RemoteRepository parseRemoteRepository(String spec);

    /**
     * Similar to {@link #parseRemoteRepository(String)} but will equip resulting repository with any possible
     * authentication needed to deploy.
     */
    RemoteRepository parseDeploymentRemoteRepository(String spec);

    /**
     * Shorthand method, creates {@link ResolutionRoot} our of passed in artifact.
     */
    default ResolutionRoot loadGav(String gav) throws ArtifactDescriptorException {
        return loadGav(gav, Collections.emptyList());
    }

    /**
     * Shorthand method, creates {@link ResolutionRoot} our of passed in artifact and BOMs.
     */
    ResolutionRoot loadGav(String gav, Collection<String> boms) throws ArtifactDescriptorException;

    /**
     * Processes the passed in instance, populates it if needed, etc.
     */
    ResolutionRoot loadRoot(ResolutionRoot resolutionRoot) throws ArtifactDescriptorException;

    /**
     * Collects given, maybe even non-existent, {@link Artifact} with all the specified dependencies, managed
     * dependencies for given resolution scope.
     */
    CollectResult collect(
            ResolutionScope resolutionScope,
            Artifact root,
            List<Dependency> dependencies,
            List<Dependency> managedDependencies,
            boolean verbose)
            throws DependencyCollectionException;

    /**
     * Collects given existing {@link Dependency} by reusing POM information for given resolution scope.
     */
    CollectResult collect(
            ResolutionScope resolutionScope,
            Dependency root,
            List<Dependency> dependencies,
            List<Dependency> managedDependencies,
            boolean verbose)
            throws DependencyCollectionException;

    /**
     * Resolves given, maybe even non-existent, {@link Artifact} with all the specified dependencies, managed
     * dependencies for given resolution scope.
     */
    DependencyResult resolve(
            ResolutionScope resolutionScope,
            Artifact root,
            List<Dependency> dependencies,
            List<Dependency> managedDependencies)
            throws DependencyResolutionException;

    /**
     * Resolves given existing {@link Dependency} by reusing POM information for given resolution scope.
     */
    DependencyResult resolve(
            ResolutionScope resolutionScope,
            Dependency root,
            List<Dependency> dependencies,
            List<Dependency> managedDependencies)
            throws DependencyResolutionException;

    /**
     * Resolves given artifacts from given remote repositories.
     */
    List<ArtifactResult> resolveArtifacts(Collection<Artifact> artifacts) throws ArtifactResolutionException;

    Version findNewestVersion(Artifact artifact, boolean allowSnapshots) throws VersionRangeResolutionException;

    List<Artifact> listAvailablePlugins(Collection<String> groupIds);
}
