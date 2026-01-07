/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

import eu.maveniverse.maven.mima.extensions.mmr.ModelResponse;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionResolutionException;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;

public interface ToolboxResolver {
    ArtifactDescriptorResult readArtifactDescriptor(Artifact artifact) throws ArtifactDescriptorException;

    ModelResponse readModel(Artifact artifact)
            throws ArtifactDescriptorException, ArtifactResolutionException, VersionResolutionException;

    List<Dependency> importBOMs(Collection<String> boms) throws ArtifactDescriptorException;

    Artifact parseGav(String gav, List<Dependency> managedDependencies);

    RemoteRepository parseRemoteRepository(String spec);

    ResolutionRoot loadGav(String gav, Collection<String> boms)
            throws InvalidVersionSpecificationException, VersionRangeResolutionException, ArtifactDescriptorException;

    ResolutionRoot loadRoot(ResolutionRoot resolutionRoot)
            throws InvalidVersionSpecificationException, VersionRangeResolutionException, ArtifactDescriptorException;

    CollectResult collect(
            ResolutionScope resolutionScope,
            Artifact root,
            List<Dependency> dependencies,
            List<Dependency> managedDependencies,
            boolean verbose)
            throws DependencyCollectionException;

    CollectResult collect(
            ResolutionScope resolutionScope,
            Artifact root,
            List<Dependency> dependencies,
            List<Dependency> managedDependencies,
            int dirtyMaxLevel,
            boolean verbose)
            throws DependencyCollectionException;

    CollectResult collect(
            ResolutionScope resolutionScope,
            Dependency root,
            List<Dependency> dependencies,
            List<Dependency> managedDependencies,
            boolean verbose)
            throws DependencyCollectionException;

    CollectResult collect(
            ResolutionScope resolutionScope,
            Dependency root,
            List<Dependency> dependencies,
            List<Dependency> managedDependencies,
            int dirtyMaxLevel,
            boolean verbose)
            throws DependencyCollectionException;

    CollectResult collectDirty(
            ResolutionScope resolutionScope,
            Artifact root,
            List<Dependency> dependencies,
            List<Dependency> managedDependencies,
            int dirtyLevelPast,
            boolean conflictResolve)
            throws DependencyCollectionException;

    CollectResult collectDirty(
            ResolutionScope resolutionScope,
            Dependency root,
            List<Dependency> dependencies,
            List<Dependency> managedDependencies,
            int dirtyLevelPast,
            boolean conflictResolve)
            throws DependencyCollectionException;

    CollectResult collectDm(Artifact root, List<Dependency> managedDependencies, boolean verbose)
            throws ArtifactDescriptorException, ArtifactResolutionException, VersionResolutionException;

    CollectResult collectDm(Dependency root, List<Dependency> managedDependencies, boolean verbose)
            throws ArtifactDescriptorException, ArtifactResolutionException, VersionResolutionException;

    CollectResult parentChildTree(ReactorLocator reactorLocator);

    CollectResult subprojectTree(ReactorLocator reactorLocator);

    List<DependencyNode> projectDependencyTree(
            ReactorLocator reactorLocator,
            boolean showExternal,
            ArtifactMatcher excludeSubprojectsMatcher,
            DependencyMatcher excludeDependencyMatcher);

    DependencyResult resolve(ResolutionScope resolutionScope, ResolutionRoot resolutionRoot)
            throws DependencyResolutionException;

    DependencyResult resolve(
            ResolutionScope resolutionScope,
            Artifact root,
            List<Dependency> dependencies,
            List<Dependency> managedDependencies)
            throws DependencyResolutionException;

    DependencyResult resolve(
            ResolutionScope resolutionScope,
            Dependency root,
            List<Dependency> dependencies,
            List<Dependency> managedDependencies)
            throws DependencyResolutionException;

    ArtifactResult resolveArtifact(Artifact artifact) throws ArtifactResolutionException;

    List<ArtifactResult> resolveArtifacts(Collection<Artifact> artifacts) throws ArtifactResolutionException;

    Artifact mayResolveArtifactVersion(
            Artifact artifact, BiFunction<Artifact, List<Version>, String> artifactVersionSelector)
            throws InvalidVersionSpecificationException, VersionRangeResolutionException;

    Version findNewestVersion(Artifact artifact, Predicate<Version> filter) throws VersionRangeResolutionException;

    List<Version> findNewerVersions(Artifact artifact, Predicate<Version> filter)
            throws VersionRangeResolutionException;

    List<Artifact> listAvailablePlugins(Collection<String> groupIds) throws Exception;
}
