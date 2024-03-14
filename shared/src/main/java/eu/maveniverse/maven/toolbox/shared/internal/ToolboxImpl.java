/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.toolbox.shared.ResolutionScope;
import eu.maveniverse.maven.toolbox.shared.Toolbox;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToolboxImpl implements Toolbox {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Context context;

    public ToolboxImpl(Context context) {
        this.context = requireNonNull(context);
    }

    @Override
    public CollectResult collect(
            ResolutionScope resolutionScope,
            Artifact root,
            List<Dependency> dependencies,
            List<Dependency> managedDependencies,
            List<RemoteRepository> remoteRepositories,
            boolean verbose)
            throws DependencyCollectionException {
        return doCollect(resolutionScope, null, root, dependencies, managedDependencies, remoteRepositories, verbose);
    }

    @Override
    public CollectResult collect(
            ResolutionScope resolutionScope,
            Dependency root,
            List<Dependency> dependencies,
            List<Dependency> managedDependencies,
            List<RemoteRepository> remoteRepositories,
            boolean verbose)
            throws DependencyCollectionException {
        return doCollect(resolutionScope, root, null, dependencies, managedDependencies, remoteRepositories, verbose);
    }

    @Override
    public DependencyResult resolve(
            ResolutionScope resolutionScope,
            Artifact root,
            List<Dependency> dependencies,
            List<Dependency> managedDependencies,
            List<RemoteRepository> remoteRepositories)
            throws DependencyResolutionException {
        return doResolve(resolutionScope, null, root, dependencies, managedDependencies, remoteRepositories);
    }

    @Override
    public DependencyResult resolve(
            ResolutionScope resolutionScope,
            Dependency root,
            List<Dependency> dependencies,
            List<Dependency> managedDependencies,
            List<RemoteRepository> remoteRepositories)
            throws DependencyResolutionException {
        return doResolve(resolutionScope, root, null, dependencies, managedDependencies, remoteRepositories);
    }

    private CollectResult doCollect(
            ResolutionScope resolutionScope,
            Dependency rootDependency,
            Artifact root,
            List<Dependency> dependencies,
            List<Dependency> managedDependencies,
            List<RemoteRepository> remoteRepositories,
            boolean verbose)
            throws DependencyCollectionException {
        requireNonNull(resolutionScope);
        if (rootDependency == null && root == null) {
            throw new NullPointerException("one of rootDependency or root must be non-null");
        }

        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession(context.repositorySystemSession());
        if (verbose) {
            session.setConfigProperty(ConflictResolver.CONFIG_PROP_VERBOSE, ConflictResolver.Verbosity.FULL);
            session.setConfigProperty(DependencyManagerUtils.CONFIG_PROP_VERBOSE, true);
        }
        session.setDependencySelector(resolutionScope.getDependencySelector());
        session.setDependencyGraphTransformer(resolutionScope.getDependencyGraphTransformer());
        logger.info("Collecting scope: {}", resolutionScope.getId());
        logger.info("   scope manager: {}", resolutionScope.getScopeManager().getDescription());

        CollectRequest collectRequest = new CollectRequest();
        if (rootDependency != null) {
            collectRequest.setRoot(rootDependency);
        } else {
            collectRequest.setRootArtifact(root);
        }
        collectRequest.setDependencies(dependencies);
        collectRequest.setManagedDependencies(managedDependencies);

        collectRequest.setRepositories(remoteRepositories);
        collectRequest.setRequestContext("project");
        collectRequest.setTrace(RequestTrace.newChild(null, collectRequest));

        logger.debug("Collecting {}", collectRequest);
        CollectResult result = context.repositorySystem().collectDependencies(session, collectRequest);
        return resolutionScope.postProcess(result);
    }

    private DependencyResult doResolve(
            ResolutionScope resolutionScope,
            Dependency rootDependency,
            Artifact root,
            List<Dependency> dependencies,
            List<Dependency> managedDependencies,
            List<RemoteRepository> remoteRepositories)
            throws DependencyResolutionException {
        requireNonNull(resolutionScope);
        if (rootDependency == null && root == null) {
            throw new NullPointerException("one of rootDependency or root must be non-null");
        }

        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession(context.repositorySystemSession());
        session.setDependencySelector(resolutionScope.getDependencySelector());
        session.setDependencyGraphTransformer(resolutionScope.getDependencyGraphTransformer());
        logger.info("Resolving scope: {}", resolutionScope.getId());
        logger.info("  scope manager: {}", resolutionScope.getScopeManager().getDescription());

        CollectRequest collectRequest = new CollectRequest();
        if (rootDependency != null) {
            collectRequest.setRoot(rootDependency);
        } else {
            collectRequest.setRootArtifact(root);
            collectRequest.setDependencies(dependencies);
            collectRequest.setManagedDependencies(managedDependencies);
        }
        collectRequest.setRepositories(remoteRepositories);
        collectRequest.setRequestContext("project");
        collectRequest.setTrace(RequestTrace.newChild(null, collectRequest));
        DependencyRequest dependencyRequest =
                new DependencyRequest(collectRequest, resolutionScope.getDependencyFilter());

        logger.debug("Resolving {}", dependencyRequest);
        return context.repositorySystem().resolveDependencies(session, dependencyRequest);
    }

    @Override
    public List<ArtifactResult> resolveArtifacts(List<Artifact> artifacts, List<RemoteRepository> remoteRepositories)
            throws ArtifactResolutionException {
        requireNonNull(artifacts);

        List<ArtifactRequest> artifactRequests = new ArrayList<>();
        artifacts.forEach(a -> artifactRequests.add(new ArtifactRequest(a, remoteRepositories, null)));
        return context.repositorySystem().resolveArtifacts(context.repositorySystemSession(), artifactRequests);
    }
}
