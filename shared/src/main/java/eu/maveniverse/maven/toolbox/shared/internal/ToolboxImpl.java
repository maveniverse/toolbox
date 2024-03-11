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
import eu.maveniverse.maven.toolbox.shared.Atoms;
import eu.maveniverse.maven.toolbox.shared.Toolbox;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import org.eclipse.aether.util.filter.ScopeDependencyFilter;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.eclipse.aether.util.graph.visitor.CloningDependencyVisitor;
import org.eclipse.aether.util.graph.visitor.FilteringDependencyVisitor;
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
            Atoms.LanguageResolutionScope resolutionScope,
            Artifact root,
            List<Dependency> dependencies,
            List<Dependency> managedDependencies,
            List<RemoteRepository> remoteRepositories,
            boolean verbose)
            throws DependencyCollectionException {
        requireNonNull(resolutionScope);

        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession(context.repositorySystemSession());
        if (verbose) {
            session.setConfigProperty(ConflictResolver.CONFIG_PROP_VERBOSE, ConflictResolver.Verbosity.FULL);
            session.setConfigProperty(DependencyManagerUtils.CONFIG_PROP_VERBOSE, true);
        }
        Set<String> includes = calculateIncludes(resolutionScope);
        Set<String> transitiveExcludes = calculateTransitiveExcludes(resolutionScope);
        logger.info(
                "Collecting project scope: {}",
                resolutionScope.getProjectScope().getId());
        logger.info(
                "        resolution scope: {}",
                resolutionScope.getResolutionScope().getId());
        logger.info(
                "                language: {}", resolutionScope.getLanguage().getId());
        logger.info("                includes: {}", includes);
        logger.info("                excludes: {}", transitiveExcludes);

        session.setDependencySelector(new AndDependencySelector(
                resolutionScope.getResolutionMode() == Atoms.ResolutionMode.ELIMINATE
                        ? ScopeDependencySelector.fromTo(2, 3, includes, null)
                        : ScopeDependencySelector.fromTo(1, 3, includes, null),
                ScopeDependencySelector.fromDirect(null, transitiveExcludes),
                OptionalDependencySelector.fromDirect(),
                new ExclusionDependencySelector()));

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRootArtifact(root);
        collectRequest.setDependencies(dependencies);
        collectRequest.setManagedDependencies(managedDependencies);
        collectRequest.setRepositories(remoteRepositories);
        collectRequest.setRequestContext("project");
        collectRequest.setTrace(RequestTrace.newChild(null, collectRequest));

        logger.debug("Collecting {}", collectRequest);
        CollectResult result = context.repositorySystem().collectDependencies(session, collectRequest);
        if (resolutionScope.getResolutionMode() == Atoms.ResolutionMode.ELIMINATE) {
            CloningDependencyVisitor cloning = new CloningDependencyVisitor();
            FilteringDependencyVisitor filter =
                    new FilteringDependencyVisitor(cloning, new ScopeDependencyFilter(includes, null));
            result.getRoot().accept(filter);
            result.setRoot(cloning.getRootNode());
        }
        return result;
    }

    @Override
    public CollectResult collect(
            Atoms.LanguageResolutionScope resolutionScope,
            Dependency root,
            List<RemoteRepository> remoteRepositories,
            boolean verbose)
            throws DependencyCollectionException {
        requireNonNull(resolutionScope);
        requireNonNull(root);

        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession(context.repositorySystemSession());
        if (verbose) {
            session.setConfigProperty(ConflictResolver.CONFIG_PROP_VERBOSE, ConflictResolver.Verbosity.FULL);
            session.setConfigProperty(DependencyManagerUtils.CONFIG_PROP_VERBOSE, true);
        }
        Set<String> includes = calculateIncludes(resolutionScope);
        Set<String> transitiveExcludes = calculateTransitiveExcludes(resolutionScope);
        logger.info(
                "Collecting project scope: {}",
                resolutionScope.getProjectScope().getId());
        logger.info(
                "        resolution scope: {}",
                resolutionScope.getResolutionScope().getId());
        logger.info(
                "                language: {}", resolutionScope.getLanguage().getId());
        logger.info("                includes: {}", includes);
        logger.info("                excludes: {}", transitiveExcludes);

        session.setDependencySelector(new AndDependencySelector(
                resolutionScope.getResolutionMode() == Atoms.ResolutionMode.ELIMINATE
                        ? ScopeDependencySelector.fromTo(2, 3, includes, null)
                        : ScopeDependencySelector.fromTo(1, 3, includes, null),
                ScopeDependencySelector.fromDirect(null, transitiveExcludes),
                OptionalDependencySelector.fromDirect(),
                new ExclusionDependencySelector()));

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(root);
        collectRequest.setRepositories(remoteRepositories);
        collectRequest.setRequestContext("project");
        collectRequest.setTrace(RequestTrace.newChild(null, collectRequest));

        logger.debug("Collecting {}", collectRequest);
        CollectResult result = context.repositorySystem().collectDependencies(session, collectRequest);
        if (resolutionScope.getResolutionMode() == Atoms.ResolutionMode.ELIMINATE) {
            CloningDependencyVisitor cloning = new CloningDependencyVisitor();
            FilteringDependencyVisitor filter =
                    new FilteringDependencyVisitor(cloning, new ScopeDependencyFilter(includes, null));
            result.getRoot().accept(filter);
            result.setRoot(cloning.getRootNode());
        }
        return result;
    }

    /**
     * This method basically translates "scope" to {@link Atoms.LanguageDependencyScope} specific string IDs.
     */
    private Set<String> calculateIncludes(Atoms.LanguageResolutionScope scope) {
        Atoms.ProjectScope projectScope = scope.getProjectScope();
        Atoms.ResolutionScope resolutionScope = scope.getResolutionScope();
        HashSet<String> includes = new HashSet<>();
        for (Atoms.LanguageDependencyScope languageDependencyScope :
                scope.getLanguage().getLanguageDependencyScopeUniverse()) {
            if (languageDependencyScope.getProjectScopes().contains(projectScope)
                    && resolutionScope.getContains().contains(languageDependencyScope.getDependencyScope())) {
                // IF: project scope is contained in given language project scopes
                // AND if resolution scope contains given language scope dependency scope
                // the languageScope should be included, add it
                includes.add(languageDependencyScope.getId());
            }
        }
        return includes;
    }

    /**
     * This method basically translates "scope" to {@link Atoms.LanguageDependencyScope} specific string IDs.
     */
    private Set<String> calculateTransitiveExcludes(Atoms.LanguageResolutionScope scope) {
        Atoms.ProjectScope projectScope = scope.getProjectScope();
        Atoms.ResolutionScope resolutionScope = scope.getResolutionScope();
        HashSet<String> excludes = new HashSet<>();
        for (Atoms.LanguageDependencyScope languageDependencyScope :
                scope.getLanguage().getLanguageDependencyScopeUniverse()) {
            // if the language scope is not meant to be here (is not present in this project scope)
            if (!languageDependencyScope.getProjectScopes().contains(projectScope)) {
                excludes.add(languageDependencyScope.getId());
                continue;
            }
            // if the language scope is not transitive
            if (!languageDependencyScope.isTransitive()) {
                excludes.add(languageDependencyScope.getId());
                continue;
            }
            // if the resolution scope explicitly excludes language scope's dependency scope
            if (resolutionScope.getExcludesTransitively().contains(languageDependencyScope.getDependencyScope())) {
                excludes.add(languageDependencyScope.getId());
            }
        }
        return excludes;
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
