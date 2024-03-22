/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;
import static org.apache.maven.search.api.request.BooleanQuery.and;
import static org.apache.maven.search.api.request.FieldQuery.fieldQuery;
import static org.apache.maven.search.api.request.Query.query;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.HTTPProxy;
import eu.maveniverse.maven.mima.context.MavenSystemHome;
import eu.maveniverse.maven.mima.context.MavenUserHome;
import eu.maveniverse.maven.mima.context.Runtime;
import eu.maveniverse.maven.mima.context.internal.RuntimeSupport;
import eu.maveniverse.maven.toolbox.shared.ArtifactSink;
import eu.maveniverse.maven.toolbox.shared.Output;
import eu.maveniverse.maven.toolbox.shared.ResolutionRoot;
import eu.maveniverse.maven.toolbox.shared.ResolutionScope;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import org.apache.maven.search.api.MAVEN;
import org.apache.maven.search.api.SearchBackend;
import org.apache.maven.search.api.SearchRequest;
import org.apache.maven.search.api.SearchResponse;
import org.apache.maven.search.api.request.Query;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.installation.InstallRequest;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.ChecksumUtils;
import org.eclipse.aether.util.artifact.SubArtifact;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;
import org.eclipse.aether.util.graph.visitor.TreeDependencyVisitor;
import org.eclipse.aether.util.listener.ChainedRepositoryListener;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.VersionConstraint;
import org.eclipse.aether.version.VersionScheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToolboxCommandoImpl implements ToolboxCommando {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Runtime runtime;
    private final Context context;
    private final ToolboxSearchApiImpl toolboxSearchApi;
    private final ArtifactRecorderImpl artifactRecorder;
    private final ToolboxResolverImpl toolboxResolver;

    public ToolboxCommandoImpl(Runtime runtime, Context context) {
        this.runtime = requireNonNull(runtime, "runtime");
        this.context = requireNonNull(context, "context");
        this.toolboxSearchApi = new ToolboxSearchApiImpl();
        this.artifactRecorder = new ArtifactRecorderImpl();
        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession(context.repositorySystemSession());
        session.setRepositoryListener(
                ChainedRepositoryListener.newInstance(session.getRepositoryListener(), artifactRecorder));
        this.toolboxResolver =
                new ToolboxResolverImpl(context.repositorySystem(), session, context.remoteRepositories());
    }

    @Override
    public ResolutionRoot loadGav(String gav, Collection<String> boms) throws ArtifactDescriptorException {
        return toolboxResolver.loadGav(gav, boms);
    }

    @Override
    public String getVersion() {
        return discoverArtifactVersion("eu.maveniverse.maven.toolbox", "shared", "unknown");
    }

    @Override
    public boolean dump(boolean verbose, Output output) {
        output.warn("Toolbox {} (MIMA Runtime '{}' version {})", getVersion(), runtime.name(), runtime.version());
        output.warn("=======");
        output.normal("          Maven version {}", runtime.mavenVersion());
        output.normal("                Managed {}", runtime.managedRepositorySystem());
        output.normal("                Basedir {}", context.basedir());
        output.normal(
                "                Offline {}", context.repositorySystemSession().isOffline());

        MavenSystemHome mavenSystemHome = context.mavenSystemHome();
        output.normal("");
        output.normal("             MAVEN_HOME {}", mavenSystemHome == null ? "undefined" : mavenSystemHome.basedir());
        if (mavenSystemHome != null) {
            output.normal("           settings.xml {}", mavenSystemHome.settingsXml());
            output.normal("         toolchains.xml {}", mavenSystemHome.toolchainsXml());
        }

        MavenUserHome mavenUserHome = context.mavenUserHome();
        output.normal("");
        output.normal("              USER_HOME {}", mavenUserHome.basedir());
        output.normal("           settings.xml {}", mavenUserHome.settingsXml());
        output.normal("  settings-security.xml {}", mavenUserHome.settingsSecurityXml());
        output.normal("       local repository {}", mavenUserHome.localRepository());

        output.normal("");
        output.normal("               PROFILES");
        output.normal("                 Active {}", context.contextOverrides().getActiveProfileIds());
        output.normal("               Inactive {}", context.contextOverrides().getInactiveProfileIds());

        output.normal("");
        output.normal("    REMOTE REPOSITORIES");
        for (RemoteRepository repository : context.remoteRepositories()) {
            if (repository.getMirroredRepositories().isEmpty()) {
                output.normal("                        {}", repository);
            } else {
                output.normal("                        {}, mirror of", repository);
                for (RemoteRepository mirrored : repository.getMirroredRepositories()) {
                    output.normal("                          {}", mirrored);
                }
            }
        }

        if (context.httpProxy() != null) {
            HTTPProxy proxy = context.httpProxy();
            output.normal("");
            output.normal("             HTTP PROXY");
            output.normal("                    url {}://{}:{}", proxy.getProtocol(), proxy.getHost(), proxy.getPort());
            output.normal("          nonProxyHosts {}", proxy.getNonProxyHosts());
        }

        if (verbose) {
            output.verbose("");
            output.verbose("        USER PROPERTIES");
            context.contextOverrides().getUserProperties().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> output.verbose("                         {}={}", e.getKey(), e.getValue()));
            output.verbose("      SYSTEM PROPERTIES");
            context.contextOverrides().getSystemProperties().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> output.verbose("                         {}={}", e.getKey(), e.getValue()));
            output.verbose("      CONFIG PROPERTIES");
            context.contextOverrides().getConfigProperties().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> output.verbose("                         {}={}", e.getKey(), e.getValue()));
            output.verbose("");

            output.normal("OUTPUT TEST:");
            output.verbose("Verbose: {}", "Message", new RuntimeException("runtime"));
            output.normal("Normal: {}", "Message", new RuntimeException("runtime"));
            output.warn("Warning: {}", "Message", new RuntimeException("runtime"));
            output.error("Error: {}", "Message", new RuntimeException("runtime"));
        }
        return true;
    }

    @Override
    public boolean classpath(ResolutionScope resolutionScope, ResolutionRoot resolutionRoot, Output output)
            throws Exception {
        output.verbose("Resolving {}", resolutionRoot.getArtifact());
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
        return !nlg.getFiles().isEmpty();
    }

    @Override
    public boolean copy(Collection<Artifact> artifacts, ArtifactSink sink, Output output) throws Exception {
        output.verbose("Resolving {}", artifacts);
        List<ArtifactResult> resolveResult = toolboxResolver.resolveArtifacts(artifacts);
        sink.accept(resolveResult.stream().map(ArtifactResult::getArtifact).collect(Collectors.toList()));
        return !resolveResult.isEmpty();
    }

    @Override
    public boolean copyTransitive(
            ResolutionScope resolutionScope,
            Collection<ResolutionRoot> resolutionRoots,
            ArtifactSink sink,
            Output output)
            throws Exception {
        ArrayList<ArtifactResult> artifactResults = new ArrayList<>();
        for (ResolutionRoot resolutionRoot : resolutionRoots) {
            output.verbose("Resolving {}", resolutionRoot.getArtifact());
            DependencyResult dependencyResult = toolboxResolver.resolve(
                    resolutionScope,
                    resolutionRoot.getArtifact(),
                    resolutionRoot.getDependencies(),
                    resolutionRoot.getManagedDependencies());
            List<ArtifactResult> adjustedResults = resolutionRoot.isLoad()
                    ? dependencyResult.getArtifactResults()
                    : dependencyResult
                            .getArtifactResults()
                            .subList(1, dependencyResult.getArtifactResults().size() - 1);
            artifactResults.addAll(adjustedResults);
        }
        sink.accept(artifactResults.stream().map(ArtifactResult::getArtifact).collect(Collectors.toList()));
        return !artifactResults.isEmpty();
    }

    @Override
    public boolean deploy(String remoteRepositorySpec, Supplier<Collection<Artifact>> artifactSupplier, Output output)
            throws Exception {
        Collection<Artifact> artifacts = artifactSupplier.get();
        RemoteRepository remoteRepository = toolboxResolver.parseDeploymentRemoteRepository(remoteRepositorySpec);
        DeployRequest deployRequest = new DeployRequest();
        deployRequest.setRepository(remoteRepository);
        artifacts.forEach(deployRequest::addArtifact);
        context.repositorySystem().deploy(context.repositorySystemSession(), deployRequest);
        output.normal("");
        output.normal("Deployed {} artifacts to {}", artifacts.size(), remoteRepository);
        return !artifacts.isEmpty();
    }

    @Override
    public boolean deployAllRecorded(String remoteRepositorySpec, boolean stopRecording, Output output)
            throws Exception {
        artifactRecorder.setActive(!stopRecording);
        return deploy(remoteRepositorySpec, () -> new HashSet<>(artifactRecorder.getAllArtifacts()), output);
    }

    @Override
    public boolean install(Supplier<Collection<Artifact>> artifactSupplier, Output output) throws Exception {
        Collection<Artifact> artifacts = artifactSupplier.get();
        InstallRequest installRequest = new InstallRequest();
        artifacts.forEach(installRequest::addArtifact);
        context.repositorySystem().install(context.repositorySystemSession(), installRequest);
        output.normal("");
        output.normal("Install {} artifacts to local repository", artifacts.size());
        return !artifacts.isEmpty();
    }

    @Override
    public boolean listRepositories(ResolutionScope resolutionScope, ResolutionRoot resolutionRoot, Output output)
            throws Exception {
        output.verbose("Loading root of: {}", resolutionRoot.getArtifact());
        ResolutionRoot root = toolboxResolver.loadRoot(resolutionRoot);
        output.verbose("Collecting graph of: {}", resolutionRoot.getArtifact());
        CollectResult collectResult = toolboxResolver.collect(
                resolutionScope, root.getArtifact(), root.getDependencies(), root.getManagedDependencies(), false);
        final HashMap<RemoteRepository, Artifact> repositories = new HashMap<>();
        Artifact sentinel = new DefaultArtifact("sentinel:sentinel:sentinel");
        context.remoteRepositories().forEach(r -> repositories.put(r, sentinel));
        final ArrayDeque<Artifact> path = new ArrayDeque<>();
        collectResult.getRoot().accept(new TreeDependencyVisitor(new DependencyVisitor() {
            @Override
            public boolean visitEnter(DependencyNode node) {
                Artifact parent = path.peek() == null ? sentinel : path.peek();
                node.getRepositories().forEach(r -> repositories.putIfAbsent(r, parent));
                path.push(node.getArtifact());
                return true;
            }

            @Override
            public boolean visitLeave(DependencyNode node) {
                path.pop();
                return true;
            }
        }));
        repositories.forEach((k, v) -> {
            output.normal(k.toString());
            output.normal("  First introduced on {}", v == sentinel ? "root" : v);
        });
        return !repositories.isEmpty();
    }

    @Override
    public boolean listAvailablePlugins(Collection<String> groupIds, Output output) throws Exception {
        output.verbose("Listing plugins in groupIds: {}", groupIds);
        List<Artifact> plugins = toolboxResolver.listAvailablePlugins(groupIds);
        plugins.forEach(p -> output.normal(p.toString()));
        return !plugins.isEmpty();
    }

    @Override
    public boolean recordStart(Output output) {
        output.verbose("Starting recorder...");
        artifactRecorder.clear();
        return artifactRecorder.setActive(true);
    }

    @Override
    public boolean recordStop(Output output) {
        output.verbose("Stopping recorder...");
        boolean result = artifactRecorder.setActive(false);
        output.verbose(
                "Recorded {} artifacts", artifactRecorder.getAllArtifacts().size());
        return result;
    }

    @Override
    public boolean resolve(
            Collection<Artifact> artifacts, boolean sources, boolean javadoc, boolean signature, Output output)
            throws Exception {
        output.verbose("Resolving {}", artifacts);
        toolboxResolver.resolveArtifacts(artifacts);
        if (sources || javadoc || signature) {
            HashSet<Artifact> subartifacts = new HashSet<>();
            artifacts.forEach(a -> {
                if (sources && a.getClassifier().isEmpty()) {
                    subartifacts.add(new SubArtifact(a, "sources", "jar"));
                }
                if (javadoc && a.getClassifier().isEmpty()) {
                    subartifacts.add(new SubArtifact(a, "javadoc", "jar"));
                }
                if (signature && !a.getExtension().endsWith(".asc")) {
                    subartifacts.add(new SubArtifact(a, "*", "*.asc"));
                }
            });
            if (!subartifacts.isEmpty()) {
                output.verbose("Resolving (best effort) {}", subartifacts);
                try {
                    toolboxResolver.resolveArtifacts(subartifacts);
                } catch (ArtifactResolutionException e) {
                    // ignore, this is "best effort"
                }
            }
        }
        return !artifacts.isEmpty();
    }

    @Override
    public boolean resolveTransitive(
            ResolutionScope resolutionScope,
            Collection<ResolutionRoot> resolutionRoots,
            boolean sources,
            boolean javadoc,
            boolean signature,
            Output output)
            throws Exception {
        int totalArtifactCount = 0;
        long totalArtifactSize = 0;
        for (ResolutionRoot resolutionRoot : resolutionRoots) {
            output.verbose("Resolving {}", resolutionRoot.getArtifact());
            DependencyResult dependencyResult = toolboxResolver.resolve(
                    resolutionScope,
                    resolutionRoot.getArtifact(),
                    resolutionRoot.getDependencies(),
                    resolutionRoot.getManagedDependencies());
            List<ArtifactResult> adjustedResults = resolutionRoot.isLoad()
                    ? dependencyResult.getArtifactResults()
                    : dependencyResult
                            .getArtifactResults()
                            .subList(1, dependencyResult.getArtifactResults().size() - 1);
            for (ArtifactResult artifactResult : adjustedResults) {
                ModuleDescriptor moduleDescriptor =
                        getModuleDescriptor(output, artifactResult.getArtifact().getFile());
                String moduleInfo = "";
                if (moduleDescriptor != null) {
                    moduleInfo = "-- module " + moduleDescriptor.name;
                    if (moduleDescriptor.automatic) {
                        if ("MANIFEST".equals(moduleDescriptor.moduleNameSource)) {
                            moduleInfo += " [auto]";
                        } else {
                            moduleInfo += " (auto)";
                        }
                    }
                }
                if (output.isVerbose()) {
                    output.verbose(
                            "{} {} -> {}",
                            artifactResult.getArtifact(),
                            moduleInfo,
                            artifactResult.getArtifact().getFile());
                } else {
                    output.normal("{} {}", artifactResult.getArtifact(), moduleInfo);
                }
            }
            int artifactCount = adjustedResults.size();
            long artifactSize = adjustedResults.stream()
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
                    .getSum();
            totalArtifactCount += artifactCount;
            totalArtifactSize += artifactSize;

            output.normal("");
            output.normal(
                    "Resolved artifact: {} (artifact count: {}, artifact size: {})",
                    resolutionRoot.getArtifact(),
                    artifactCount,
                    humanReadableByteCountBin(artifactSize));
        }
        output.normal("====================");
        output.normal(
                "Total resolved artifacts: {} (artifact count: {}, artifact size: {})",
                resolutionRoots.size(),
                totalArtifactCount,
                humanReadableByteCountBin(totalArtifactSize));
        return true;
    }

    @Override
    public boolean tree(
            ResolutionScope resolutionScope, ResolutionRoot resolutionRoot, boolean verbose, Output output) {
        try {
            output.verbose("Loading root of: {}", resolutionRoot.getArtifact());
            ResolutionRoot root = toolboxResolver.loadRoot(resolutionRoot);
            output.verbose("Collecting graph of: {}", resolutionRoot.getArtifact());
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
    public Map<String, RemoteRepository> getKnownRemoteRepositories() {
        return toolboxSearchApi.getKnownRemoteRepositories();
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

    // Utils

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

    public static String discoverArtifactVersion(String groupId, String artifactId, String defVal) {
        Map<String, String> mavenPomProperties = loadPomProperties(groupId, artifactId);
        String versionString = mavenPomProperties.getOrDefault("version", "").trim();
        if (!versionString.startsWith("${")) {
            return versionString;
        }
        return defVal;
    }

    public static Map<String, String> loadPomProperties(String groupId, String artifactId) {
        return loadClasspathProperties("/META-INF/maven/" + groupId + "/" + artifactId + "/pom.properties");
    }

    public static Map<String, String> loadClasspathProperties(String resource) {
        final Properties props = new Properties();
        try (InputStream is = RuntimeSupport.class.getResourceAsStream(resource)) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException e) {
            // fall through
        }
        return props.entrySet().stream()
                .collect(toMap(
                        e -> String.valueOf(e.getKey()),
                        e -> String.valueOf(e.getValue()),
                        (prev, next) -> next,
                        HashMap::new));
    }

    public static ModuleDescriptor getModuleDescriptor(Output output, File artifactFile) {
        ModuleDescriptor moduleDescriptor = null;
        try {
            // Use Java9 code to get moduleName, don't try to do it better with own implementation
            Class<?> moduleFinderClass = Class.forName("java.lang.module.ModuleFinder");

            Path path = artifactFile.toPath();

            Method ofMethod = moduleFinderClass.getMethod("of", java.nio.file.Path[].class);
            Object moduleFinderInstance = ofMethod.invoke(null, new Object[] {new java.nio.file.Path[] {path}});

            Method findAllMethod = moduleFinderClass.getMethod("findAll");
            Set<Object> moduleReferences = (Set<Object>) findAllMethod.invoke(moduleFinderInstance);

            // moduleReferences can be empty when referring to target/classes without module-info.class
            if (!moduleReferences.isEmpty()) {
                Object moduleReference = moduleReferences.iterator().next();
                Method descriptorMethod = moduleReference.getClass().getMethod("descriptor");
                Object moduleDescriptorInstance = descriptorMethod.invoke(moduleReference);

                Method nameMethod = moduleDescriptorInstance.getClass().getMethod("name");
                String name = (String) nameMethod.invoke(moduleDescriptorInstance);

                moduleDescriptor = new ModuleDescriptor();
                moduleDescriptor.name = name;

                Method isAutomaticMethod = moduleDescriptorInstance.getClass().getMethod("isAutomatic");
                moduleDescriptor.automatic = (Boolean) isAutomaticMethod.invoke(moduleDescriptorInstance);

                if (moduleDescriptor.automatic) {
                    if (artifactFile.isFile()) {
                        try (JarFile jarFile = new JarFile(artifactFile)) {
                            Manifest manifest = jarFile.getManifest();

                            if (manifest != null
                                    && manifest.getMainAttributes().getValue("Automatic-Module-Name") != null) {
                                moduleDescriptor.moduleNameSource = "MANIFEST";
                            } else {
                                moduleDescriptor.moduleNameSource = "FILENAME";
                            }
                        } catch (IOException e) {
                            // noop
                        }
                    }
                }
            }
        } catch (ClassNotFoundException | SecurityException | IllegalAccessException | IllegalArgumentException e) {
            output.verbose("Ignored Exception:", e);
        } catch (NoSuchMethodException e) {
            output.verbose("Ignored Exception:", e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            output.verbose("Can't extract module name from {}:", artifactFile.getName(), cause);
        }
        return moduleDescriptor;
    }

    public static class ModuleDescriptor {
        String name;
        boolean automatic = true;
        String moduleNameSource;
    }
}
