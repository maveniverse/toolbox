/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static eu.maveniverse.maven.toolbox.shared.ArtifactVersionSelector.last;
import static eu.maveniverse.maven.toolbox.shared.internal.ToolboxCommandoImpl.source;
import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mima.extensions.mmr.MavenModelReader;
import eu.maveniverse.maven.mima.extensions.mmr.ModelRequest;
import eu.maveniverse.maven.mima.extensions.mmr.ModelResponse;
import eu.maveniverse.maven.toolbox.shared.ArtifactMatcher;
import eu.maveniverse.maven.toolbox.shared.ArtifactVersionMatcher;
import eu.maveniverse.maven.toolbox.shared.DependencyMatcher;
import eu.maveniverse.maven.toolbox.shared.ProjectLocator;
import eu.maveniverse.maven.toolbox.shared.ReactorLocator;
import eu.maveniverse.maven.toolbox.shared.ResolutionRoot;
import eu.maveniverse.maven.toolbox.shared.ResolutionScope;
import eu.maveniverse.maven.toolbox.shared.ToolboxResolver;
import eu.maveniverse.maven.toolbox.shared.output.Output;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.metadata.DefaultMetadata;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.resolution.MetadataRequest;
import org.eclipse.aether.resolution.MetadataResult;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.resolution.VersionResolutionException;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.eclipse.aether.util.repository.SimpleArtifactDescriptorPolicy;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionConstraint;
import org.eclipse.aether.version.VersionScheme;

public class ToolboxResolverImpl implements ToolboxResolver {
    protected static final String CTX_TOOLBOX = "toolbox";
    protected final Output output;
    protected final RepositorySystem repositorySystem;
    protected final RepositorySystemSession session;
    protected final MavenModelReader mavenModelReader;
    protected final ProjectLocator projectLocator;
    protected final List<RemoteRepository> remoteRepositories;
    protected final VersionScheme versionScheme;

    public ToolboxResolverImpl(
            Output output,
            RepositorySystem repositorySystem,
            RepositorySystemSession session,
            MavenModelReader mavenModelReader,
            List<RemoteRepository> remoteRepositories,
            VersionScheme versionScheme) {
        this.output = requireNonNull(output, "output");
        this.repositorySystem = requireNonNull(repositorySystem, "repositorySystem");
        this.session = requireNonNull(session, "session");
        this.mavenModelReader = requireNonNull(mavenModelReader, "mavenModelReader");
        this.projectLocator = new ProjectLocatorImpl(session, mavenModelReader);
        this.remoteRepositories = requireNonNull(remoteRepositories, "remoteRepositories");
        this.versionScheme = requireNonNull(versionScheme, "versionScheme");
    }

    @Override
    public ArtifactDescriptorResult readArtifactDescriptor(Artifact artifact) throws ArtifactDescriptorException {
        ArtifactDescriptorRequest artifactDescriptorRequest =
                new ArtifactDescriptorRequest(artifact, remoteRepositories, CTX_TOOLBOX);
        return repositorySystem.readArtifactDescriptor(session, artifactDescriptorRequest);
    }

    @Override
    public ModelResponse readModel(Artifact artifact)
            throws ArtifactDescriptorException, ArtifactResolutionException, VersionResolutionException {
        return mavenModelReader.readModel(ModelRequest.builder()
                .setArtifact(artifact)
                .setRequestContext(CTX_TOOLBOX)
                .build());
    }

    @Override
    public List<Dependency> importBOMs(Collection<String> boms) throws ArtifactDescriptorException {
        HashSet<String> keys = new HashSet<>();
        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession(this.session);
        session.setArtifactDescriptorPolicy(new SimpleArtifactDescriptorPolicy(false, false));
        ArrayList<Dependency> managedDependencies = new ArrayList<>();
        for (String bomGav : boms) {
            if (null == bomGav || bomGav.isEmpty()) {
                continue;
            }
            Artifact bom = new DefaultArtifact(bomGav);
            ArtifactDescriptorResult artifactDescriptorResult = readArtifactDescriptor(bom);
            artifactDescriptorResult.getManagedDependencies().forEach(d -> {
                if (keys.add(ArtifactIdUtils.toVersionlessId(d.getArtifact()))) {
                    managedDependencies.add(d);
                } else {
                    output.warn("BOM {} introduced an already managed dependency {}", bom, d);
                }
            });
        }
        return managedDependencies;
    }

    @Override
    public Artifact parseGav(String gav, List<Dependency> managedDependencies) {
        try {
            return new DefaultArtifact(gav);
        } catch (IllegalArgumentException e) {
            if (managedDependencies != null) {
                // assume it is g:a and we have v in depMgt section
                return managedDependencies.stream()
                        .map(Dependency::getArtifact)
                        .filter(a -> gav.equals(a.getGroupId() + ":" + a.getArtifactId()))
                        .findFirst()
                        .orElseThrow(() -> e);
            } else {
                throw e;
            }
        }
    }

    @Override
    public RemoteRepository parseRemoteRepository(String spec) {
        String[] parts = spec.split("::");
        String id = "mima";
        String type = "default";
        String url;
        if (parts.length == 1) {
            url = parts[0];
        } else if (parts.length == 2) {
            id = parts[0];
            url = parts[1];
        } else if (parts.length == 3) {
            id = parts[0];
            type = parts[1];
            url = parts[2];
        } else {
            throw new IllegalArgumentException("Invalid remote repository spec");
        }
        return repositorySystem.newDeploymentRepository(session, new RemoteRepository.Builder(id, type, url).build());
    }

    @Override
    public ResolutionRoot loadGav(String gav, Collection<String> boms)
            throws InvalidVersionSpecificationException, VersionRangeResolutionException, ArtifactDescriptorException {
        List<Dependency> managedDependency = importBOMs(boms);
        Artifact artifact = parseGav(gav, managedDependency);
        return loadRoot(ResolutionRoot.ofLoaded(artifact)
                .withManagedDependencies(managedDependency)
                .build());
    }

    @Override
    public ResolutionRoot loadRoot(ResolutionRoot resolutionRoot)
            throws InvalidVersionSpecificationException, VersionRangeResolutionException, ArtifactDescriptorException {
        if (resolutionRoot.isPrepared()) {
            return resolutionRoot;
        }
        if (resolutionRoot.isLoad()) {
            Artifact resolvedVersionArtifact = mayResolveArtifactVersion(resolutionRoot.getArtifact(), last());
            ArtifactDescriptorResult artifactDescriptorResult = readArtifactDescriptor(resolvedVersionArtifact);
            resolutionRoot = ResolutionRoot.ofLoaded(resolvedVersionArtifact)
                    .applyManagedDependencies(resolutionRoot.isApplyManagedDependencies())
                    .cutDependencies(resolutionRoot.isCutDependencies())
                    .withDependencies(
                            mergeDeps(resolutionRoot.getDependencies(), artifactDescriptorResult.getDependencies()))
                    .withManagedDependencies(mergeDeps(
                            resolutionRoot.getManagedDependencies(), artifactDescriptorResult.getManagedDependencies()))
                    .build();
        } else {
            if (versionScheme
                            .parseVersionConstraint(resolutionRoot.getArtifact().getVersion())
                            .getRange()
                    != null) {
                throw new IllegalArgumentException(
                        "non-loaded resolution root artifact version must be simple version but is range: "
                                + resolutionRoot.getArtifact());
            }
        }
        return resolutionRoot.prepared();
    }

    protected List<Dependency> mergeDeps(List<Dependency> dominant, List<Dependency> recessive) {
        List<Dependency> result;
        if (dominant == null || dominant.isEmpty()) {
            result = recessive;
        } else if (recessive == null || recessive.isEmpty()) {
            result = dominant;
        } else {
            int initialCapacity = dominant.size() + recessive.size();
            result = new ArrayList<>(initialCapacity);
            Collection<String> ids = new HashSet<>(initialCapacity, 1.0f);
            for (Dependency dependency : dominant) {
                ids.add(getId(dependency.getArtifact()));
                result.add(dependency);
            }
            for (Dependency dependency : recessive) {
                if (!ids.contains(getId(dependency.getArtifact()))) {
                    result.add(dependency);
                }
            }
        }
        return result;
    }

    protected static String getId(Artifact a) {
        return a.getGroupId() + ':' + a.getArtifactId() + ':' + a.getClassifier() + ':' + a.getExtension();
    }

    @Override
    public CollectResult collect(
            ResolutionScope resolutionScope,
            Artifact root,
            List<Dependency> dependencies,
            List<Dependency> managedDependencies,
            boolean verbose)
            throws DependencyCollectionException {
        return doCollect(
                resolutionScope, null, root, dependencies, managedDependencies, remoteRepositories, -1, verbose);
    }

    @Override
    public CollectResult collect(
            ResolutionScope resolutionScope,
            Artifact root,
            List<Dependency> dependencies,
            List<Dependency> managedDependencies,
            int dirtyMaxLevel,
            boolean verbose)
            throws DependencyCollectionException {
        return doCollect(
                resolutionScope,
                null,
                root,
                dependencies,
                managedDependencies,
                remoteRepositories,
                dirtyMaxLevel,
                verbose);
    }

    @Override
    public CollectResult collect(
            ResolutionScope resolutionScope,
            Dependency root,
            List<Dependency> dependencies,
            List<Dependency> managedDependencies,
            boolean verbose)
            throws DependencyCollectionException {
        return doCollect(
                resolutionScope, root, null, dependencies, managedDependencies, remoteRepositories, -1, verbose);
    }

    @Override
    public CollectResult collect(
            ResolutionScope resolutionScope,
            Dependency root,
            List<Dependency> dependencies,
            List<Dependency> managedDependencies,
            int dirtyMaxLevel,
            boolean verbose)
            throws DependencyCollectionException {
        return doCollect(
                resolutionScope,
                root,
                null,
                dependencies,
                managedDependencies,
                remoteRepositories,
                dirtyMaxLevel,
                verbose);
    }

    @Override
    public CollectResult collectDm(Artifact root, List<Dependency> managedDependencies, boolean verbose)
            throws ArtifactDescriptorException, ArtifactResolutionException, VersionResolutionException {
        return doCollectDm(null, root, managedDependencies, remoteRepositories, verbose);
    }

    @Override
    public CollectResult collectDm(Dependency root, List<Dependency> managedDependencies, boolean verbose)
            throws ArtifactDescriptorException, ArtifactResolutionException, VersionResolutionException {
        return doCollectDm(root, null, managedDependencies, remoteRepositories, verbose);
    }

    @Override
    public CollectResult parentChildTree(ReactorLocator reactorLocator) {
        CollectRequest collectRequest = new CollectRequest();
        ProjectLocator.Project startingProject = reactorLocator.getSelectedOrCurrentProject();
        collectRequest.setRoot(new Dependency(source(startingProject), ""));
        CollectResult result = new CollectResult(collectRequest);

        ProjectLocator.Project currentProject = startingProject;
        result.setRoot(new DefaultDependencyNode(collectRequest.getRoot()));
        DependencyNode node;
        DependencyNode leaf = result.getRoot();
        Optional<Artifact> parentArtifact = currentProject.getParent();
        while (parentArtifact.isPresent()) {
            Artifact pa = parentArtifact.orElseThrow();
            Optional<ReactorLocator.ReactorProject> parentProject = reactorLocator.locateProject(pa);
            if (parentProject.isPresent()) {
                currentProject = parentProject.orElseThrow();
                node = new DefaultDependencyNode(new Dependency(source(currentProject), ""));
            } else {
                Optional<? extends ProjectLocator.Project> external = projectLocator.locateProject(pa);
                if (external.isPresent()) {
                    currentProject = external.orElseThrow();
                    node = new DefaultDependencyNode(new Dependency(source(currentProject), ""));
                } else {
                    currentProject = null;
                    node = new DefaultDependencyNode(new Dependency(pa, ""));
                }
            }
            node.getChildren().add(result.getRoot());
            result.setRoot(node);

            parentArtifact = currentProject != null ? currentProject.getParent() : Optional.empty();
        }
        doParentChildTree(leaf, reactorLocator, startingProject);
        return result;
    }

    protected void doParentChildTree(
            DependencyNode parentNode, ReactorLocator reactorLocator, ProjectLocator.Project parent) {
        List<? extends ProjectLocator.Project> children = reactorLocator.locateChildren(parent);
        for (ProjectLocator.Project child : children) {
            DependencyNode childNode = new DefaultDependencyNode(new Dependency(source(child), ""));
            parentNode.getChildren().add(childNode);
            doParentChildTree(childNode, reactorLocator, child);
        }
    }

    @Override
    public CollectResult subprojectTree(ReactorLocator reactorLocator) {
        CollectRequest collectRequest = new CollectRequest();
        ProjectLocator.Project startingProject = reactorLocator.getSelectedOrCurrentProject();
        collectRequest.setRoot(new Dependency(source(startingProject), ""));
        CollectResult result = new CollectResult(collectRequest);
        result.setRoot(new DefaultDependencyNode(collectRequest.getRoot()));
        doSubprojectTree(result.getRoot(), reactorLocator, startingProject);
        return result;
    }

    protected void doSubprojectTree(
            DependencyNode parentNode, ReactorLocator reactorLocator, ProjectLocator.Project parent) {
        List<? extends ProjectLocator.Project> children = reactorLocator.locateCollected(parent);
        for (ProjectLocator.Project child : children) {
            DependencyNode childNode = new DefaultDependencyNode(new Dependency(source(child), ""));
            parentNode.getChildren().add(childNode);
            doSubprojectTree(childNode, reactorLocator, child);
        }
    }

    @Override
    public List<DependencyNode> projectDependencyTree(
            ReactorLocator reactorLocator,
            boolean showExternal,
            ArtifactMatcher excludeSubprojectsMatcher,
            DependencyMatcher excludeDependencyMatcher) {
        ArrayList<DependencyNode> results = new ArrayList<>();
        if (reactorLocator.getSelectedProject().isPresent()) {
            results.add(doProjectDependencyTree(
                    reactorLocator,
                    reactorLocator.getSelectedProject().orElseThrow(),
                    showExternal,
                    excludeSubprojectsMatcher,
                    excludeDependencyMatcher,
                    new HashSet<>()));
        } else {
            for (ReactorLocator.ReactorProject project : reactorLocator.getAllProjects()) {
                if (!excludeSubprojectsMatcher.test(project.artifact())) {
                    results.add(doProjectDependencyTree(
                            reactorLocator,
                            project,
                            showExternal,
                            excludeSubprojectsMatcher,
                            excludeDependencyMatcher,
                            new HashSet<>()));
                }
            }
        }
        return results;
    }

    protected DependencyNode doProjectDependencyTree(
            ReactorLocator reactorLocator,
            ReactorLocator.ReactorProject project,
            boolean showExternal,
            ArtifactMatcher excludeSubprojectsMatcher,
            DependencyMatcher excludeDependencyMatcher,
            HashSet<Artifact> seen) {
        DependencyNode result = new DefaultDependencyNode(new Dependency(source(project), ""));
        for (Dependency dependency : project.dependencies()) {
            Optional<ReactorLocator.ReactorProject> rp = reactorLocator.locateProject(dependency.getArtifact());
            boolean isReactorMember = rp.isPresent();
            if (isReactorMember) {
                if (!excludeSubprojectsMatcher.test(dependency.getArtifact())
                        && !excludeDependencyMatcher.test(dependency)) {
                    if (seen.add(dependency.getArtifact())) {
                        DependencyNode child = doProjectDependencyTree(
                                reactorLocator,
                                rp.orElseThrow(),
                                showExternal,
                                excludeSubprojectsMatcher,
                                excludeDependencyMatcher,
                                seen);
                        child.setScope(dependency.getScope());
                        result.getChildren().add(child);
                    }
                }
            } else {
                if (showExternal && !excludeDependencyMatcher.test(dependency)) {
                    result.getChildren()
                            .add(new DefaultDependencyNode(
                                    dependency.setArtifact(source(dependency.getArtifact(), true))));
                }
            }
        }
        return result;
    }

    protected Artifact source(ProjectLocator.Project project) {
        return source(project.artifact(), project.origin() == this.projectLocator);
    }

    protected Artifact source(Artifact artifact, boolean external) {
        return ToolboxCommandoImpl.source(artifact, external);
    }

    @Override
    public DependencyResult resolve(ResolutionScope resolutionScope, ResolutionRoot resolutionRoot)
            throws DependencyResolutionException {
        if (resolutionRoot.isLoad()) {
            return resolve(
                    resolutionScope,
                    new Dependency(resolutionRoot.getArtifact(), ""),
                    resolutionRoot.getDependencies(),
                    resolutionRoot.getManagedDependencies());
        } else {
            return resolve(
                    resolutionScope,
                    resolutionRoot.getArtifact(),
                    resolutionRoot.getDependencies(),
                    resolutionRoot.getManagedDependencies());
        }
    }

    @Override
    public DependencyResult resolve(
            ResolutionScope resolutionScope,
            Artifact root,
            List<Dependency> dependencies,
            List<Dependency> managedDependencies)
            throws DependencyResolutionException {
        return doResolve(resolutionScope, null, root, dependencies, managedDependencies, remoteRepositories);
    }

    @Override
    public DependencyResult resolve(
            ResolutionScope resolutionScope,
            Dependency root,
            List<Dependency> dependencies,
            List<Dependency> managedDependencies)
            throws DependencyResolutionException {
        return doResolve(resolutionScope, root, null, dependencies, managedDependencies, remoteRepositories);
    }

    protected CollectResult doCollect(
            ResolutionScope resolutionScope,
            Dependency rootDependency,
            Artifact root,
            List<Dependency> dependencies,
            List<Dependency> managedDependencies,
            List<RemoteRepository> remoteRepositories,
            int dirtyMaxLevel,
            boolean verbose)
            throws DependencyCollectionException {
        requireNonNull(resolutionScope);
        if (rootDependency == null && root == null) {
            throw new NullPointerException("one of rootDependency or root must be non-null");
        }

        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession(this.session);
        session.setConfigProperty(DependencyManagerUtils.CONFIG_PROP_VERBOSE, true);
        if (verbose) {
            session.setConfigProperty(ConflictResolver.CONFIG_PROP_VERBOSE, ConflictResolver.Verbosity.STANDARD);
        }
        boolean dirtyTree = dirtyMaxLevel > 0;
        if (dirtyTree) {
            session.setDependencySelector(new LevelDependencySelector(dirtyMaxLevel));
            session.setDependencyGraphTransformer(null);
        }
        CollectRequest collectRequest = new CollectRequest();
        if (rootDependency != null) {
            root = rootDependency.getArtifact();
        }
        collectRequest.setRootArtifact(root);
        if (dirtyTree) {
            collectRequest.setDependencies(dependencies);
        } else {
            collectRequest.setDependencies(dependencies.stream()
                    .filter(d -> !resolutionScope.isEliminateTest() || !JavaScopes.TEST.equals(d.getScope()))
                    .collect(Collectors.toList()));
        }
        collectRequest.setManagedDependencies(managedDependencies);
        collectRequest.setRepositories(remoteRepositories);
        collectRequest.setRequestContext(CTX_TOOLBOX);
        collectRequest.setTrace(RequestTrace.newChild(null, collectRequest));

        output.chatter("Collecting {} @ {}", collectRequest, resolutionScope.name());
        CollectResult result = repositorySystem.collectDependencies(session, collectRequest);
        if (!dirtyTree && !verbose && resolutionScope != ResolutionScope.TEST) {
            ArrayList<DependencyNode> childrenToRemove = new ArrayList<>();
            for (DependencyNode node : result.getRoot().getChildren()) {
                if (!resolutionScope
                        .getDirectInclude()
                        .contains(node.getDependency().getScope())) {
                    childrenToRemove.add(node);
                }
            }
            if (!childrenToRemove.isEmpty()) {
                result.getRoot().getChildren().removeAll(childrenToRemove);
            }
        }
        return result;
    }

    protected CollectResult doCollectDm(
            Dependency rootDependency,
            Artifact root,
            List<Dependency> managedDependencies,
            List<RemoteRepository> remoteRepositories,
            boolean verbose)
            throws ArtifactDescriptorException, ArtifactResolutionException, VersionResolutionException {
        if (rootDependency == null && root == null) {
            throw new NullPointerException("one of rootDependency or root must be non-null");
        }

        CollectRequest collectRequest = new CollectRequest();
        if (rootDependency != null) {
            root = rootDependency.getArtifact();
        }
        collectRequest.setRootArtifact(root);
        collectRequest.setManagedDependencies(managedDependencies);
        collectRequest.setRepositories(remoteRepositories);
        collectRequest.setRequestContext(CTX_TOOLBOX);
        collectRequest.setTrace(RequestTrace.newChild(null, collectRequest));

        output.chatter("Collecting depMgt {}", root);
        CollectResult result = new CollectResult(collectRequest);
        DefaultDependencyNode rootNode =
                new DefaultDependencyNode(rootDependency != null ? rootDependency.getArtifact() : root);
        result.setRoot(rootNode);
        HashMap<String, LinkedHashSet<String>> encounters = new HashMap<>();
        doCollectDmRecursive(rootNode, encounters);
        Map<String, LinkedHashSet<String>> conflicts = encounters.entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (!conflicts.isEmpty()) {
            output.warn("DM conflicts discovered:");
            for (Map.Entry<String, LinkedHashSet<String>> entry : conflicts.entrySet()) {
                output.warn(
                        " * {} version {} prevails, but met versions {}",
                        entry.getKey(),
                        entry.getValue().iterator().next(),
                        entry.getValue());
            }
        }
        return result;
    }

    protected void doCollectDmRecursive(
            DefaultDependencyNode currentRoot, Map<String, LinkedHashSet<String>> encounters)
            throws ArtifactDescriptorException, ArtifactResolutionException, VersionResolutionException {
        ModelResponse modelResponse = mavenModelReader.readModel(ModelRequest.builder()
                .setArtifact(currentRoot.getArtifact())
                .setRequestContext(CTX_TOOLBOX)
                .build());

        Model rawModel = null;
        for (String lineage : modelResponse.getLineage()) {
            Model current = modelResponse.getLineageModel(lineage);
            if (rawModel == null) {
                rawModel = current;
            } else if (current.getDependencyManagement() != null) {
                if (rawModel.getDependencyManagement() == null) {
                    rawModel.setDependencyManagement(new DependencyManagement());
                }
                rawModel.getDependencyManagement()
                        .getDependencies()
                        .addAll(0, current.getDependencyManagement().getDependencies());
            }
        }

        for (Dependency managedDependency : modelResponse
                .toArtifactDescriptorResult(modelResponse.interpolateModel(rawModel))
                .getManagedDependencies()) {
            DefaultDependencyNode child = new DefaultDependencyNode(managedDependency);
            currentRoot.getChildren().add(child);
            String key = ArtifactIdUtils.toVersionlessId(managedDependency.getArtifact());
            encounters
                    .computeIfAbsent(key, k -> new LinkedHashSet<>())
                    .add(managedDependency.getArtifact().getVersion());
            if ("import".equals(child.getDependency().getScope())) {
                doCollectDmRecursive(child, encounters);
            }
        }
    }

    protected DependencyResult doResolve(
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

        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession(this.session);

        CollectRequest collectRequest = new CollectRequest();
        if (rootDependency != null) {
            root = rootDependency.getArtifact();
        }
        collectRequest.setRootArtifact(root);
        collectRequest.setDependencies(dependencies.stream()
                .filter(d -> !resolutionScope.isEliminateTest() || !JavaScopes.TEST.equals(d.getScope()))
                .collect(Collectors.toList()));
        collectRequest.setManagedDependencies(managedDependencies);
        collectRequest.setRepositories(remoteRepositories);
        collectRequest.setRequestContext(CTX_TOOLBOX);
        collectRequest.setTrace(RequestTrace.newChild(null, collectRequest));
        DependencyRequest dependencyRequest =
                new DependencyRequest(collectRequest, resolutionScope.getDependencyFilter());

        output.chatter("Resolving {} @ {}", dependencyRequest, resolutionScope.name());
        DependencyResult result = repositorySystem.resolveDependencies(session, dependencyRequest);
        if (rootDependency != null) {
            try {
                ArtifactResult rootResult = resolveArtifacts(Collections.singletonList(rootDependency.getArtifact()))
                        .get(0);

                DefaultDependencyNode newRoot = new DefaultDependencyNode(new Dependency(rootResult.getArtifact(), ""));
                newRoot.setChildren(result.getRoot().getChildren());
                result.setRoot(newRoot);
                result.getArtifactResults().add(0, rootResult);
                return result;
            } catch (ArtifactResolutionException e) {
                throw new DependencyResolutionException(result, e);
            }
        } else {
            return result;
        }
    }

    @Override
    public ArtifactResult resolveArtifact(Artifact artifact) throws ArtifactResolutionException {
        requireNonNull(artifact);
        return resolveArtifacts(Collections.singleton(artifact)).get(0);
    }

    @Override
    public List<ArtifactResult> resolveArtifacts(Collection<Artifact> artifacts) throws ArtifactResolutionException {
        requireNonNull(artifacts);

        List<ArtifactRequest> artifactRequests = new ArrayList<>();
        artifacts.forEach(a -> artifactRequests.add(new ArtifactRequest(a, remoteRepositories, null)));
        return repositorySystem.resolveArtifacts(session, artifactRequests);
    }

    @Override
    public Artifact mayResolveArtifactVersion(
            Artifact artifact, BiFunction<Artifact, List<Version>, String> artifactVersionSelector)
            throws InvalidVersionSpecificationException, VersionRangeResolutionException {
        String version;
        VersionConstraint versionConstraint = versionScheme.parseVersionConstraint(artifact.getVersion());
        if (versionConstraint.getRange() != null) {
            VersionRangeResult versionRangeResult = repositorySystem.resolveVersionRange(
                    session, new VersionRangeRequest(artifact, remoteRepositories, CTX_TOOLBOX));
            version = artifactVersionSelector.apply(artifact, versionRangeResult.getVersions());
        } else {
            version = versionConstraint.getVersion().toString();
        }
        return artifact.setVersion(version);
    }

    @Override
    public Version findNewestVersion(Artifact artifact, Predicate<Version> filter)
            throws VersionRangeResolutionException {
        VersionRangeRequest rangeRequest = new VersionRangeRequest();
        rangeRequest.setArtifact(new DefaultArtifact(
                artifact.getGroupId(),
                artifact.getArtifactId(),
                artifact.getClassifier(),
                artifact.getExtension(),
                "[0,)"));
        rangeRequest.setRepositories(remoteRepositories);
        rangeRequest.setRequestContext(CTX_TOOLBOX);
        VersionRangeResult result = repositorySystem.resolveVersionRange(session, rangeRequest);
        Version highest = result.getHighestVersion();
        if (filter.test(highest)) {
            return highest;
        } else {
            for (int idx = result.getVersions().size() - 1; idx >= 0; idx--) {
                highest = result.getVersions().get(idx);
                if (filter.test(highest)) {
                    return highest;
                }
            }
            return null;
        }
    }

    @Override
    public List<Version> findNewerVersions(Artifact artifact, Predicate<Version> filter)
            throws VersionRangeResolutionException {
        VersionRangeRequest rangeRequest = new VersionRangeRequest();
        rangeRequest.setArtifact(new DefaultArtifact(
                artifact.getGroupId(),
                artifact.getArtifactId(),
                artifact.getClassifier(),
                artifact.getExtension(),
                artifact.getVersion().contains(",") ? artifact.getVersion() : "(" + artifact.getVersion() + ",)"));
        rangeRequest.setRepositories(remoteRepositories);
        rangeRequest.setRequestContext(CTX_TOOLBOX);
        VersionRangeResult result = repositorySystem.resolveVersionRange(session, rangeRequest);
        return result.getVersions().stream().filter(filter).collect(Collectors.toList());
    }

    @Override
    public List<Artifact> listAvailablePlugins(Collection<String> groupIds) throws Exception {
        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession(this.session);
        session.setUpdatePolicy(RepositoryPolicy.UPDATE_POLICY_ALWAYS);

        RequestTrace trace = RequestTrace.newChild(null, this);

        List<MetadataRequest> requests = new ArrayList<>();
        for (String groupId : groupIds) {
            org.eclipse.aether.metadata.Metadata metadata =
                    new DefaultMetadata(groupId, "maven-metadata.xml", DefaultMetadata.Nature.RELEASE);
            for (RemoteRepository repository : remoteRepositories) {
                requests.add(new MetadataRequest(metadata, repository, CTX_TOOLBOX).setTrace(trace));
            }
        }

        HashSet<String> processedGAs = new HashSet<>();
        ArrayList<Artifact> result = new ArrayList<>();
        List<MetadataResult> results = repositorySystem.resolveMetadata(session, requests);
        for (MetadataResult res : results) {
            org.eclipse.aether.metadata.Metadata metadata = res.getMetadata();
            if (metadata != null
                    && metadata.getFile() != null
                    && metadata.getFile().isFile()) {
                try (InputStream inputStream =
                        Files.newInputStream(metadata.getFile().toPath())) {
                    org.apache.maven.artifact.repository.metadata.Metadata pluginGroupMetadata =
                            new MetadataXpp3Reader().read(inputStream, false);
                    List<org.apache.maven.artifact.repository.metadata.Plugin> plugins =
                            pluginGroupMetadata.getPlugins();
                    for (org.apache.maven.artifact.repository.metadata.Plugin plugin : plugins) {
                        if (processedGAs.add(metadata.getGroupId() + ":" + plugin.getArtifactId())) {
                            Artifact blueprint =
                                    new DefaultArtifact(metadata.getGroupId(), plugin.getArtifactId(), "jar", "0");
                            Version newestVersion = findNewestVersion(
                                    blueprint, ArtifactVersionMatcher.not(ArtifactVersionMatcher.snapshot()));
                            if (newestVersion != null) {
                                result.add(new DefaultArtifact(
                                        blueprint.getGroupId(),
                                        blueprint.getArtifactId(),
                                        blueprint.getExtension(),
                                        newestVersion.toString()));
                            }
                        }
                    }
                }
            }
        }
        return result;
    }
}
