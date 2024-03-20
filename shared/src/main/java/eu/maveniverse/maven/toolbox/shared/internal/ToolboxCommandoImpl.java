/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static java.util.Objects.requireNonNull;
import static org.apache.maven.search.api.request.BooleanQuery.and;
import static org.apache.maven.search.api.request.FieldQuery.fieldQuery;
import static org.apache.maven.search.api.request.Query.query;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.toolbox.shared.ArtifactRecorder;
import eu.maveniverse.maven.toolbox.shared.Output;
import eu.maveniverse.maven.toolbox.shared.ResolutionRoot;
import eu.maveniverse.maven.toolbox.shared.ResolutionScope;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import eu.maveniverse.maven.toolbox.shared.ToolboxResolver;
import eu.maveniverse.maven.toolbox.shared.ToolboxSearchApi;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.maven.search.api.MAVEN;
import org.apache.maven.search.api.SearchBackend;
import org.apache.maven.search.api.SearchRequest;
import org.apache.maven.search.api.SearchResponse;
import org.apache.maven.search.api.request.Query;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.deployment.DeploymentException;
import org.eclipse.aether.installation.InstallRequest;
import org.eclipse.aether.installation.InstallationException;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.ChecksumUtils;
import org.eclipse.aether.util.artifact.SubArtifact;
import org.eclipse.aether.util.graph.visitor.DependencyGraphDumper;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.VersionConstraint;
import org.eclipse.aether.version.VersionScheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToolboxCommandoImpl implements ToolboxCommando {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Context context;
    private final ToolboxResolverImpl toolboxResolver;
    private final ToolboxSearchApiImpl toolboxSearchApi;
    private final ArtifactRecorderImpl artifactRecorder;

    public ToolboxCommandoImpl(Context context) {
        requireNonNull(context, "context");
        this.context = context;
        this.toolboxResolver = new ToolboxResolverImpl(
                context.repositorySystem(), context.repositorySystemSession(), context.remoteRepositories());
        this.toolboxSearchApi = new ToolboxSearchApiImpl();
        this.artifactRecorder = new ArtifactRecorderImpl();
    }

    @Override
    public ToolboxCommando derive(ContextOverrides contextOverrides) {
        return new ToolboxCommandoImpl(context.customize(contextOverrides));
    }

    @Override
    public void close() {
        context.close();
    }

    @Override
    public ToolboxResolver toolboxResolver() {
        return toolboxResolver;
    }

    @Override
    public ToolboxSearchApi toolboxSearchApi() {
        return toolboxSearchApi;
    }

    @Override
    public ArtifactRecorder artifactRecorder() {
        return artifactRecorder;
    }

    @Override
    public boolean classpath(ResolutionScope resolutionScope, ResolutionRoot resolutionRoot, Output output) {
        try {
            DependencyResult dependencyResult = toolboxResolver.resolve(
                    resolutionScope,
                    resolutionRoot.getArtifact(),
                    resolutionRoot.getDependencies(),
                    resolutionRoot.getManagedDependencies());

            PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
            dependencyResult.getRoot().accept(nlg);
            // TODO: Do not use PreorderNodeListGenerator#getClassPath() until MRESOLVER-483 is fixed/released
            output.normal(
                    "{}",
                    nlg.getFiles().stream().map(File::getAbsolutePath).collect(Collectors.joining(File.pathSeparator)));
            return true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean deploy(String remoteRepositorySpec, Collection<Artifact> artifacts, Output output) {
        try {
            RemoteRepository remoteRepository = toolboxResolver.parseDeploymentRemoteRepository(remoteRepositorySpec);
            DeployRequest deployRequest = new DeployRequest();
            deployRequest.setRepository(remoteRepository);
            artifacts.forEach(deployRequest::addArtifact);
            context.repositorySystem().deploy(context.repositorySystemSession(), deployRequest);
            output.normal("");
            output.normal("Deployed {} artifacts to {}", artifacts.size(), remoteRepository);
            return !artifacts.isEmpty();
        } catch (DeploymentException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean install(Collection<Artifact> artifacts, Output output) {
        try {
            InstallRequest installRequest = new InstallRequest();
            artifacts.forEach(installRequest::addArtifact);
            context.repositorySystem().install(context.repositorySystemSession(), installRequest);
            output.normal("");
            output.normal("Install {} artifacts to local repository", artifacts.size());
            return !artifacts.isEmpty();
        } catch (InstallationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean listAvailablePlugins(Collection<String> groupIds, Output output) {
        toolboxResolver.listAvailablePlugins(groupIds).forEach(p -> output.normal(p.toString()));
        return true;
    }

    @Override
    public boolean resolve(
            ResolutionScope resolutionScope,
            ResolutionRoot resolutionRoot,
            boolean sources,
            boolean javadoc,
            boolean signatures,
            Output output) {
        try {
            DependencyResult dependencyResult = toolboxResolver.resolve(
                    resolutionScope,
                    resolutionRoot.getArtifact(),
                    resolutionRoot.getDependencies(),
                    resolutionRoot.getManagedDependencies());

            output.normal("");
            if (output.isVerbose()) {
                for (ArtifactResult artifactResult : dependencyResult.getArtifactResults()) {
                    output.verbose(
                            "{} -> {}",
                            artifactResult.getArtifact(),
                            artifactResult.getArtifact().getFile());
                }
            }
            output.normal("Resolved: {}", resolutionRoot.getArtifact());
            if (output.isVerbose()) {
                output.normal(
                        "  Transitive hull count: {}",
                        dependencyResult.getArtifactResults().size());
                output.normal(
                        "  Transitive hull size: {}",
                        humanReadableByteCountBin(dependencyResult.getArtifactResults().stream()
                                .map(ArtifactResult::getArtifact)
                                .map(Artifact::getFile)
                                .filter(Objects::nonNull)
                                .map(f -> {
                                    try {
                                        return Files.size(f.toPath());
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                })
                                .collect(Collectors.summarizingLong(Long::longValue))
                                .getSum()));
            }
            return true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String humanReadableByteCountBin(long bytes) {
        long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
        if (absB < 1024) {
            return bytes + " B";
        }
        long value = absB;
        CharacterIterator ci = new StringCharacterIterator("KMGTPE");
        for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
            value >>= 10;
            ci.next();
        }
        value *= Long.signum(bytes);
        return String.format("%.1f %ciB", value / 1024.0, ci.current());
    }

    @Override
    public boolean tree(
            ResolutionScope resolutionScope, ResolutionRoot resolutionRoot, boolean verbose, Output output) {
        try {
            ResolutionRoot root = toolboxResolver.loadRoot(resolutionRoot);
            CollectResult collectResult = toolboxResolver.collect(
                    resolutionScope,
                    root.getArtifact(),
                    root.getDependencies(),
                    root.getManagedDependencies(),
                    verbose);
            collectResult.getRoot().accept(new DependencyGraphDumper(output::normal));
            return true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean exists(
            RemoteRepository remoteRepository,
            String gav,
            boolean pom,
            boolean sources,
            boolean javadoc,
            boolean signature,
            boolean allRequired,
            Output output)
            throws IOException {
        ArrayList<Artifact> missingOnes = new ArrayList<>();
        ArrayList<Artifact> existingOnes = new ArrayList<>();
        try (SearchBackend backend = toolboxSearchApi.getRemoteRepositoryBackend(remoteRepository)) {
            Artifact artifact = new DefaultArtifact(gav);
            boolean exists = toolboxSearchApi.exists(backend, artifact);
            if (!exists) {
                missingOnes.add(artifact);
            } else {
                existingOnes.add(artifact);
            }
            output.normal("Artifact {} {}", artifact, exists ? "EXISTS" : "NOT EXISTS");
            if (pom && !"pom".equals(artifact.getExtension())) {
                Artifact poma = new SubArtifact(artifact, null, "pom");
                exists = toolboxSearchApi.exists(backend, poma);
                if (!exists && allRequired) {
                    missingOnes.add(poma);
                } else if (allRequired) {
                    existingOnes.add(poma);
                }
                output.normal("    {} {}", poma, exists ? "EXISTS" : "NOT EXISTS");
            }
            if (sources) {
                Artifact sourcesa = new SubArtifact(artifact, "sources", "jar");
                exists = toolboxSearchApi.exists(backend, sourcesa);
                if (!exists && allRequired) {
                    missingOnes.add(sourcesa);
                } else if (allRequired) {
                    existingOnes.add(sourcesa);
                }
                output.normal("    {} {}", sourcesa, exists ? "EXISTS" : "NOT EXISTS");
            }
            if (javadoc) {
                Artifact javadoca = new SubArtifact(artifact, "javadoc", "jar");
                exists = toolboxSearchApi.exists(backend, javadoca);
                if (!exists && allRequired) {
                    missingOnes.add(javadoca);
                } else if (allRequired) {
                    existingOnes.add(javadoca);
                }
                output.normal("    {} {}", javadoca, exists ? "EXISTS" : "NOT EXISTS");
            }
            if (signature) {
                Artifact signaturea = new SubArtifact(artifact, null, artifact.getExtension() + ".asc");
                exists = toolboxSearchApi.exists(backend, signaturea);
                if (!exists && allRequired) {
                    missingOnes.add(signaturea);
                } else if (allRequired) {
                    existingOnes.add(signaturea);
                }
                output.normal("    {} {}", signaturea, exists ? "EXISTS" : "NOT EXISTS");
            }
        }
        output.normal("");
        output.normal(
                "Checked TOTAL of {} (existing: {} not existing: {})",
                existingOnes.size() + missingOnes.size(),
                existingOnes.size(),
                missingOnes.size());
        return missingOnes.isEmpty();
    }

    @Override
    public boolean identify(RemoteRepository remoteRepository, String target, Output output) throws IOException {
        String sha1;
        if (Files.exists(Paths.get(target))) {
            try {
                output.verbose("Calculating SHA1 of file {}", target);
                MessageDigest sha1md = MessageDigest.getInstance("SHA-1");
                byte[] buf = new byte[8192];
                int read;
                try (FileInputStream fis = new FileInputStream(target)) {
                    read = fis.read(buf);
                    while (read != -1) {
                        sha1md.update(buf, 0, read);
                        read = fis.read(buf);
                    }
                }
                sha1 = ChecksumUtils.toHexString(sha1md.digest());
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("SHA1 MessageDigest unavailable", e);
            }
        } else {
            sha1 = target;
        }
        output.verbose("Identifying artifact with SHA1={}", sha1);
        try (SearchBackend backend = toolboxSearchApi.getSmoBackend(remoteRepository)) {
            SearchRequest searchRequest = new SearchRequest(fieldQuery(MAVEN.SHA1, sha1));
            SearchResponse searchResponse = backend.search(searchRequest);

            toolboxSearchApi.renderPage(searchResponse.getPage(), null, output);
            while (searchResponse.getCurrentHits() > 0) {
                searchResponse =
                        backend.search(searchResponse.getSearchRequest().nextPage());
                toolboxSearchApi.renderPage(searchResponse.getPage(), null, output);
            }
        }
        return true;
    }

    @Override
    public boolean list(RemoteRepository remoteRepository, String gavoid, Output output) throws IOException {
        try (SearchBackend backend = toolboxSearchApi.getRemoteRepositoryBackend(remoteRepository)) {
            String[] elements = gavoid.split(":");
            if (elements.length < 1 || elements.length > 3) {
                throw new IllegalArgumentException("Invalid gavoid");
            }

            Query query = fieldQuery(MAVEN.GROUP_ID, elements[0]);
            if (elements.length > 1) {
                query = and(query, fieldQuery(MAVEN.ARTIFACT_ID, elements[1]));
            }

            VersionScheme versionScheme = new GenericVersionScheme();
            Predicate<String> versionPredicate = null;
            if (elements.length > 2) {
                try {
                    VersionConstraint versionConstraint = versionScheme.parseVersionConstraint(elements[2]);
                    if (versionConstraint.getRange() != null) {
                        versionPredicate = s -> {
                            try {
                                return versionConstraint.containsVersion(versionScheme.parseVersion(s));
                            } catch (InvalidVersionSpecificationException e) {
                                return false;
                            }
                        };
                    }
                } catch (InvalidVersionSpecificationException e) {
                    // ignore and continue as before
                }
                if (versionPredicate == null) {
                    query = and(query, fieldQuery(MAVEN.VERSION, elements[2]));
                }
            }
            SearchRequest searchRequest = new SearchRequest(query);
            SearchResponse searchResponse = backend.search(searchRequest);

            toolboxSearchApi.renderPage(searchResponse.getPage(), versionPredicate, output);
        }
        return true;
    }

    @Override
    public boolean search(RemoteRepository remoteRepository, String expression, Output output) throws IOException {
        try (SearchBackend backend = toolboxSearchApi.getSmoBackend(remoteRepository)) {
            Query query;
            try {
                query = toolboxSearchApi.toSmoQuery(new DefaultArtifact(expression));
            } catch (IllegalArgumentException e) {
                query = query(expression);
            }
            SearchRequest searchRequest = new SearchRequest(query);
            SearchResponse searchResponse = backend.search(searchRequest);

            toolboxSearchApi.renderPage(searchResponse.getPage(), null, output);
            while (searchResponse.getCurrentHits() > 0) {
                searchResponse =
                        backend.search(searchResponse.getSearchRequest().nextPage());
                toolboxSearchApi.renderPage(searchResponse.getPage(), null, output);
            }
        }
        return true;
    }

    @Override
    public boolean verify(RemoteRepository remoteRepository, String gav, String sha1, Output output)
            throws IOException {
        try (SearchBackend backend = toolboxSearchApi.getRemoteRepositoryBackend(remoteRepository)) {
            Artifact artifact = new DefaultArtifact(gav);
            boolean verified = toolboxSearchApi.verify(backend, new DefaultArtifact(gav), sha1);
            output.normal("Artifact SHA1({})={}: {}", artifact, sha1, verified ? "MATCHED" : "NOT MATCHED");
            return verified;
        }
    }
}
