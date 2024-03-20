/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.toolbox.shared.ResolutionRoot;
import eu.maveniverse.maven.toolbox.shared.ResolutionScope;
import eu.maveniverse.maven.toolbox.shared.ToolboxResolver;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
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
import org.eclipse.aether.util.artifact.ArtifactIdUtils;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.eclipse.aether.util.repository.SimpleArtifactDescriptorPolicy;
import org.eclipse.aether.version.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToolboxResolverImpl implements ToolboxResolver {
    private static final String CTX_TOOLBOX = "toolbox";
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final RepositorySystem repositorySystem;
    private final RepositorySystemSession session;
    private final List<RemoteRepository> remoteRepositories;

    public ToolboxResolverImpl(
            RepositorySystem repositorySystem,
            RepositorySystemSession session,
            List<RemoteRepository> remoteRepositories) {
        this.repositorySystem = requireNonNull(repositorySystem, "repositorySystem");
        this.session = requireNonNull(session, "session");
        this.remoteRepositories = requireNonNull(remoteRepositories, "remoteRepositories");
    }

    @Override
    public ArtifactDescriptorResult readArtifactDescriptor(Artifact artifact) throws ArtifactDescriptorException {
        ArtifactDescriptorRequest artifactDescriptorRequest =
                new ArtifactDescriptorRequest(artifact, remoteRepositories, CTX_TOOLBOX);
        return repositorySystem.readArtifactDescriptor(session, artifactDescriptorRequest);
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
                    logger.warn("BOM {} introduced an already managed dependency {}", bom, d);
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
        if (parts.length == 1) {
            return new RemoteRepository.Builder("mima", "default", parts[0]).build();
        } else if (parts.length == 2) {
            return new RemoteRepository.Builder(parts[0], "default", parts[1]).build();
        } else {
            throw new IllegalArgumentException("Invalid remote repository spec");
        }
    }

    @Override
    public RemoteRepository parseDeploymentRemoteRepository(String spec) {
        return repositorySystem.newDeploymentRepository(session, parseRemoteRepository(spec));
    }

    @Override
    public ResolutionRoot loadGav(String gav, Collection<String> boms) throws ArtifactDescriptorException {
        List<Dependency> managedDependency = importBOMs(boms);
        Artifact artifact = parseGav(gav, managedDependency);
        return loadRoot(ResolutionRoot.ofLoaded(artifact)
                .withDependencies(managedDependency)
                .build());
    }

    @Override
    public ResolutionRoot loadRoot(ResolutionRoot resolutionRoot) throws ArtifactDescriptorException {
        if (!resolutionRoot.isLoad()) {
            return resolutionRoot;
        }
        ArtifactDescriptorResult artifactDescriptorResult = readArtifactDescriptor(resolutionRoot.getArtifact());

        return ResolutionRoot.ofNotLoaded(resolutionRoot.getArtifact())
                .withDependencies(
                        mergeDeps(resolutionRoot.getDependencies(), artifactDescriptorResult.getDependencies()))
                .withManagedDependencies(mergeDeps(
                        resolutionRoot.getManagedDependencies(), artifactDescriptorResult.getManagedDependencies()))
                .build();
    }

    private List<Dependency> mergeDeps(List<Dependency> dominant, List<Dependency> recessive) {
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

    private static String getId(Artifact a) {
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
        return doCollect(resolutionScope, null, root, dependencies, managedDependencies, remoteRepositories, verbose);
    }

    @Override
    public CollectResult collect(
            ResolutionScope resolutionScope,
            Dependency root,
            List<Dependency> dependencies,
            List<Dependency> managedDependencies,
            boolean verbose)
            throws DependencyCollectionException {
        return doCollect(resolutionScope, root, null, dependencies, managedDependencies, remoteRepositories, verbose);
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

        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession(this.session);
        if (verbose) {
            session.setConfigProperty(ConflictResolver.CONFIG_PROP_VERBOSE, ConflictResolver.Verbosity.FULL);
            session.setConfigProperty(DependencyManagerUtils.CONFIG_PROP_VERBOSE, true);
        }
        logger.debug("Resolving scope: {}", resolutionScope.name());

        CollectRequest collectRequest = new CollectRequest();
        if (rootDependency != null) {
            try {
                root = rootDependency.getArtifact();
                ArtifactDescriptorResult artifactDescriptorResult =
                        readArtifactDescriptor(rootDependency.getArtifact());
                root = artifactDescriptorResult.getArtifact();
                if (dependencies == null) {
                    dependencies = new ArrayList<>();
                }
                dependencies.addAll(artifactDescriptorResult.getDependencies());
                if (managedDependencies == null) {
                    managedDependencies = new ArrayList<>();
                }
                managedDependencies.addAll(artifactDescriptorResult.getManagedDependencies());
            } catch (ArtifactDescriptorException e) {
                // skip
            }
        }
        collectRequest.setRootArtifact(root);
        collectRequest.setDependencies(dependencies.stream()
                .filter(d -> resolutionScope.getDirectInclude().test(d.getScope()))
                .collect(Collectors.toList()));
        collectRequest.setManagedDependencies(managedDependencies);
        collectRequest.setRepositories(remoteRepositories);
        collectRequest.setRequestContext(CTX_TOOLBOX);
        collectRequest.setTrace(RequestTrace.newChild(null, collectRequest));

        logger.debug("Collecting {}", collectRequest);
        return repositorySystem.collectDependencies(session, collectRequest);
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

        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession(this.session);
        logger.debug("Resolving scope: {}", resolutionScope.name());

        CollectRequest collectRequest = new CollectRequest();
        if (rootDependency != null) {
            try {
                root = rootDependency.getArtifact();
                ArtifactDescriptorResult artifactDescriptorResult =
                        readArtifactDescriptor(rootDependency.getArtifact());
                root = artifactDescriptorResult.getArtifact();
                if (dependencies == null) {
                    dependencies = new ArrayList<>();
                }
                dependencies.addAll(artifactDescriptorResult.getDependencies());
                if (managedDependencies == null) {
                    managedDependencies = new ArrayList<>();
                }
                managedDependencies.addAll(artifactDescriptorResult.getManagedDependencies());
            } catch (ArtifactDescriptorException e) {
                // skip
            }
        }
        collectRequest.setRootArtifact(root);
        collectRequest.setDependencies(dependencies.stream()
                .filter(d -> resolutionScope.getDirectInclude().test(d.getScope()))
                .collect(Collectors.toList()));
        collectRequest.setManagedDependencies(managedDependencies);
        collectRequest.setRepositories(remoteRepositories);
        collectRequest.setRequestContext(CTX_TOOLBOX);
        collectRequest.setTrace(RequestTrace.newChild(null, collectRequest));
        DependencyRequest dependencyRequest =
                new DependencyRequest(collectRequest, resolutionScope.getDependencyFilter());

        logger.debug("Resolving {}", dependencyRequest);
        return repositorySystem.resolveDependencies(session, dependencyRequest);
    }

    @Override
    public List<ArtifactResult> resolveArtifacts(List<Artifact> artifacts) throws ArtifactResolutionException {
        requireNonNull(artifacts);

        List<ArtifactRequest> artifactRequests = new ArrayList<>();
        artifacts.forEach(a -> artifactRequests.add(new ArtifactRequest(a, remoteRepositories, null)));
        return repositorySystem.resolveArtifacts(session, artifactRequests);
    }

    @Override
    public Version findNewestVersion(Artifact artifact, boolean allowSnapshots) throws VersionRangeResolutionException {
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
        if (allowSnapshots) {
            return result.getHighestVersion();
        } else {
            int idx = result.getVersions().size() - 1;
            Version highest = result.getVersions().get(idx);
            while (highest.toString().endsWith("SNAPSHOT")) {
                idx -= 1;
                highest = result.getVersions().get(idx);
            }
            return highest;
        }
    }

    @Override
    public List<Artifact> listAvailablePlugins(List<String> groupIds) {
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
                            Version newestVersion = findNewestVersion(blueprint, false);
                            result.add(new DefaultArtifact(
                                    blueprint.getGroupId(),
                                    blueprint.getArtifactId(),
                                    blueprint.getExtension(),
                                    newestVersion.toString()));
                        }
                    }
                } catch (IOException | XmlPullParserException | VersionRangeResolutionException e) {
                    // skip
                }
            }
        }
        return result;
    }
}
