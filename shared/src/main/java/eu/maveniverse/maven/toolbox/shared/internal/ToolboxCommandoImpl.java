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
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.HTTPProxy;
import eu.maveniverse.maven.mima.context.MavenSystemHome;
import eu.maveniverse.maven.mima.context.MavenUserHome;
import eu.maveniverse.maven.mima.context.Runtime;
import eu.maveniverse.maven.mima.context.internal.RuntimeSupport;
import eu.maveniverse.maven.mima.extensions.mmr.MavenModelReader;
import eu.maveniverse.maven.toolbox.shared.ArtifactMapper;
import eu.maveniverse.maven.toolbox.shared.ArtifactMatcher;
import eu.maveniverse.maven.toolbox.shared.ArtifactNameMapper;
import eu.maveniverse.maven.toolbox.shared.ArtifactVersionMatcher;
import eu.maveniverse.maven.toolbox.shared.ArtifactVersionSelector;
import eu.maveniverse.maven.toolbox.shared.DependencyMatcher;
import eu.maveniverse.maven.toolbox.shared.ResolutionRoot;
import eu.maveniverse.maven.toolbox.shared.ResolutionScope;
import eu.maveniverse.maven.toolbox.shared.Result;
import eu.maveniverse.maven.toolbox.shared.Sink;
import eu.maveniverse.maven.toolbox.shared.Source;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.maven.search.api.MAVEN;
import org.apache.maven.search.api.SearchBackend;
import org.apache.maven.search.api.SearchRequest;
import org.apache.maven.search.api.SearchResponse;
import org.apache.maven.search.api.request.Query;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.util.ChecksumUtils;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;
import org.eclipse.aether.util.artifact.SubArtifact;
import org.eclipse.aether.util.graph.visitor.PathRecordingDependencyVisitor;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;
import org.eclipse.aether.util.graph.visitor.TreeDependencyVisitor;
import org.eclipse.aether.util.listener.ChainedRepositoryListener;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionConstraint;
import org.eclipse.aether.version.VersionScheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToolboxCommandoImpl implements ToolboxCommando {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Context context;
    private final RepositorySystemSession session;
    private final VersionScheme versionScheme;
    private final ToolboxSearchApiImpl toolboxSearchApi;
    private final ArtifactRecorderImpl artifactRecorder;
    private final ToolboxResolverImpl toolboxResolver;

    private final Map<String, RemoteRepository> knownSearchRemoteRepositories;

    public ToolboxCommandoImpl(Context context) {
        this.context = requireNonNull(context, "context");
        this.versionScheme = new GenericVersionScheme();
        this.toolboxSearchApi = new ToolboxSearchApiImpl();
        this.artifactRecorder = new ArtifactRecorderImpl();
        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession(context.repositorySystemSession());
        session.setRepositoryListener(
                ChainedRepositoryListener.newInstance(session.getRepositoryListener(), artifactRecorder));
        this.session = session;
        this.toolboxResolver = new ToolboxResolverImpl(
                context.repositorySystem(),
                session,
                new MavenModelReader(context),
                context.remoteRepositories(),
                versionScheme);
        this.knownSearchRemoteRepositories = Collections.unmodifiableMap(createKnownSearchRemoteRepositories());
    }

    public Path basedir() {
        return context.basedir();
    }

    public RepositorySystem repositorySystem() {
        return context.repositorySystem();
    }

    public RepositorySystemSession session() {
        return session;
    }

    public List<RemoteRepository> remoteRepositories() {
        return context.remoteRepositories();
    }

    protected Map<String, RemoteRepository> createKnownSearchRemoteRepositories() {
        Map<String, RemoteRepository> rr = new HashMap<>();
        rr.put(
                ContextOverrides.CENTRAL.getId(),
                parseRemoteRepository(
                        ContextOverrides.CENTRAL.getId() + "::central::" + ContextOverrides.CENTRAL.getUrl()));
        rr.put(
                "sonatype-oss-releases",
                parseRemoteRepository(
                        "sonatype-oss-releases::nx2::https://oss.sonatype.org/content/repositories/releases/"));
        rr.put(
                "sonatype-oss-staging",
                parseRemoteRepository("sonatype-oss-staging::nx2::https://oss.sonatype.org/content/groups/staging/"));
        rr.put(
                "sonatype-s01-releases",
                parseRemoteRepository(
                        "sonatype-s01-releases::nx2::https://s01.oss.sonatype.org/content/repositories/releases/"));
        rr.put(
                "sonatype-s01-staging",
                parseRemoteRepository(
                        "sonatype-s01-staging::nx2::https://s01.oss.sonatype.org/content/groups/staging/"));
        rr.put(
                "apache-releases",
                parseRemoteRepository(
                        "apache-releases::nx2::https://repository.apache.org/content/repositories/releases/"));
        rr.put(
                "apache-staging",
                parseRemoteRepository("apache-staging::nx2::https://repository.apache.org/content/groups/staging/"));
        rr.put(
                "apache-maven-staging",
                parseRemoteRepository(
                        "apache-maven-staging::nx2::https://repository.apache.org/content/groups/maven-staging-group/"));
        return rr;
    }

    @Override
    public Result<String> dump(Logger output) {
        Runtime runtime = context.getRuntime();
        output.info("Toolbox {} (MIMA Runtime '{}' version {})", getVersion(), runtime.name(), runtime.version());
        output.info("=======");
        output.info("          Maven version {}", runtime.mavenVersion());
        output.info("                Managed {}", runtime.managedRepositorySystem());
        output.info("                Basedir {}", context.basedir());
        output.info(
                "                Offline {}", context.repositorySystemSession().isOffline());

        MavenSystemHome mavenSystemHome = context.mavenSystemHome();
        output.info("");
        output.info("             MAVEN_HOME {}", mavenSystemHome == null ? "undefined" : mavenSystemHome.basedir());
        if (mavenSystemHome != null) {
            output.info("           settings.xml {}", mavenSystemHome.settingsXml());
            output.info("         toolchains.xml {}", mavenSystemHome.toolchainsXml());
        }

        MavenUserHome mavenUserHome = context.mavenUserHome();
        output.info("");
        output.info("              USER_HOME {}", mavenUserHome.basedir());
        output.info("           settings.xml {}", mavenUserHome.settingsXml());
        output.info("  settings-security.xml {}", mavenUserHome.settingsSecurityXml());
        output.info("       local repository {}", mavenUserHome.localRepository());

        output.info("");
        output.info("               PROFILES");
        output.info("                 Active {}", context.contextOverrides().getActiveProfileIds());
        output.info("               Inactive {}", context.contextOverrides().getInactiveProfileIds());

        output.info("");
        output.info("    REMOTE REPOSITORIES");
        for (RemoteRepository repository : context.remoteRepositories()) {
            if (repository.getMirroredRepositories().isEmpty()) {
                output.info("                        {}", repository);
            } else {
                output.info("                        {}, mirror of", repository);
                for (RemoteRepository mirrored : repository.getMirroredRepositories()) {
                    output.info("                          {}", mirrored);
                }
            }
        }

        if (context.httpProxy() != null) {
            HTTPProxy proxy = context.httpProxy();
            output.info("");
            output.info("             HTTP PROXY");
            output.info("                    url {}://{}:{}", proxy.getProtocol(), proxy.getHost(), proxy.getPort());
            output.info("          nonProxyHosts {}", proxy.getNonProxyHosts());
        }

        if (output.isDebugEnabled()) {
            output.debug("");
            output.debug("        USER PROPERTIES");
            context.repositorySystemSession().getUserProperties().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> output.debug("                         {}={}", e.getKey(), e.getValue()));
            output.debug("      SYSTEM PROPERTIES");
            context.repositorySystemSession().getSystemProperties().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> output.debug("                         {}={}", e.getKey(), e.getValue()));
            output.debug("      CONFIG PROPERTIES");
            context.repositorySystemSession().getConfigProperties().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> output.debug("                         {}={}", e.getKey(), e.getValue()));
            output.debug("");

            output.debug("OUTPUT TEST:");
            output.trace("Verbose: {}", "Message", new RuntimeException("runtime"));
            output.debug("Normal: {}", "Message", new RuntimeException("runtime"));
            output.warn("Warning: {}", "Message", new RuntimeException("runtime"));
            output.error("Error: {}", "Message", new RuntimeException("runtime"));
        }
        return Result.success("Success");
    }

    @Override
    public ArtifactMapper parseArtifactMapperSpec(String spec) {
        return ArtifactMapper.build(context.repositorySystemSession().getConfigProperties(), spec);
    }

    @Override
    public ArtifactMatcher parseArtifactMatcherSpec(String spec) {
        return ArtifactMatcher.build(context.repositorySystemSession().getConfigProperties(), spec);
    }

    @Override
    public ArtifactNameMapper parseArtifactNameMapperSpec(String spec) {
        return ArtifactNameMapper.build(context.repositorySystemSession().getConfigProperties(), spec);
    }

    @Override
    public DependencyMatcher parseDependencyMatcherSpec(String spec) {
        return DependencyMatcher.build(context.repositorySystemSession().getConfigProperties(), spec);
    }

    @Override
    public ArtifactVersionMatcher parseArtifactVersionMatcherSpec(String spec) {
        return ArtifactVersionMatcher.build(
                versionScheme, context.repositorySystemSession().getConfigProperties(), spec);
    }

    @Override
    public ArtifactVersionSelector parseArtifactVersionSelectorSpec(String spec) {
        return ArtifactVersionSelector.build(
                versionScheme, context.repositorySystemSession().getConfigProperties(), spec);
    }

    @Override
    public RemoteRepository parseRemoteRepository(String spec) {
        return toolboxResolver.parseRemoteRepository(spec);
    }

    @Override
    public Sink<Artifact> artifactSink(String spec) {
        return ArtifactSinks.build(context.repositorySystemSession().getConfigProperties(), this, spec);
    }

    @Override
    public Sink<Dependency> dependencySink(String spec) {
        return DependencySinks.build(context.repositorySystemSession().getConfigProperties(), this, spec);
    }

    @Override
    public ResolutionRoot loadGav(String gav, Collection<String> boms)
            throws InvalidVersionSpecificationException, VersionRangeResolutionException, ArtifactDescriptorException {
        return toolboxResolver.loadGav(gav, boms);
    }

    @Override
    public Artifact toArtifact(Dependency dependency) {
        try {
            // TODO: this is how Maven behaves, make it configurable?
            return toolboxResolver.mayResolveArtifactVersion(dependency.getArtifact(), ArtifactVersionSelector.last());
        } catch (InvalidVersionSpecificationException | VersionRangeResolutionException e) {
            logger.warn("Could not resolve artifact version: {}", dependency.getArtifact(), e);
            return dependency.getArtifact();
        }
    }

    @Override
    public Result<String> classpath(ResolutionScope resolutionScope, ResolutionRoot resolutionRoot, Logger output)
            throws Exception {
        output.debug("Resolving {}", resolutionRoot.getArtifact());
        resolutionRoot = toolboxResolver.loadRoot(resolutionRoot);
        DependencyResult dependencyResult = toolboxResolver.resolve(
                resolutionScope,
                resolutionRoot.getArtifact(),
                resolutionRoot.getDependencies(),
                resolutionRoot.getManagedDependencies());

        PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
        dependencyResult.getRoot().accept(nlg);
        String classpath = nlg.getClassPath();
        output.info(classpath);
        if (nlg.getFiles().isEmpty()) {
            return Result.failure("No files");
        } else {
            return Result.success(classpath);
        }
    }

    @Override
    public Result<List<Artifact>> copy(Source<Artifact> source, Sink<Artifact> sink, Logger output) throws Exception {
        try (source;
                sink) {
            List<Artifact> resolveResult = toolboxResolver
                    .resolveArtifacts(source.get().toList())
                    .stream()
                    .filter(r -> r.isResolved())
                    .map(ArtifactResult::getArtifact)
                    .toList();
            sink.accept(resolveResult);
            output.info("Resolved {} artifacts", resolveResult.size());
            return resolveResult.isEmpty() ? Result.failure("No artifacts") : Result.success(resolveResult);
        }
    }

    @Override
    public Result<List<Artifact>> copyTransitive(
            ResolutionScope resolutionScope,
            Collection<ResolutionRoot> resolutionRoots,
            Sink<Artifact> sink,
            Logger output)
            throws Exception {
        ArrayList<Artifact> artifactResults = new ArrayList<>();
        for (ResolutionRoot resolutionRoot : resolutionRoots) {
            output.debug("Resolving {}", resolutionRoot.getArtifact());
            resolutionRoot = toolboxResolver.loadRoot(resolutionRoot);
            DependencyResult dependencyResult = toolboxResolver.resolve(
                    resolutionScope,
                    resolutionRoot.getArtifact(),
                    resolutionRoot.getDependencies(),
                    resolutionRoot.getManagedDependencies());
            artifactResults.addAll((resolutionRoot.isLoad()
                            ? dependencyResult.getArtifactResults()
                            : dependencyResult
                                    .getArtifactResults()
                                    .subList(
                                            1,
                                            dependencyResult
                                                            .getArtifactResults()
                                                            .size()
                                                    - 1))
                    .stream()
                            .filter(ArtifactResult::isResolved)
                            .map(ArtifactResult::getArtifact)
                            .toList());
        }
        return copy(artifactResults::stream, sink, output);
    }

    @Override
    public Result<List<Artifact>> copyRecorded(boolean stopRecording, Sink<Artifact> sink, Logger output)
            throws Exception {
        artifactRecorder.setActive(!stopRecording);
        return copy(artifactRecorder, sink, output);
    }

    @Override
    public Result<Map<String, List<RemoteRepository>>> listRepositories(
            ResolutionScope resolutionScope, Map<String, ResolutionRoot> resolutionRoots, Logger output)
            throws Exception {
        HashMap<String, List<RemoteRepository>> result = new HashMap<>();
        for (Map.Entry<String, ResolutionRoot> entry : resolutionRoots.entrySet()) {
            String contextName = entry.getKey();
            ResolutionRoot resolutionRoot = entry.getValue();
            output.debug("Loading root of {} {}", contextName, resolutionRoot.getArtifact());
            ResolutionRoot root = toolboxResolver.loadRoot(resolutionRoot);
            output.debug("Collecting graph of: {}", resolutionRoot.getArtifact());
            CollectResult collectResult = toolboxResolver.collect(
                    resolutionScope, root.getArtifact(), root.getDependencies(), root.getManagedDependencies(), false);
            LinkedHashMap<RemoteRepository, Dependency> repositories = new LinkedHashMap<>();
            Dependency sentinel = new Dependency(new DefaultArtifact("sentinel:sentinel:sentinel"), "");
            remoteRepositories().forEach(r -> repositories.put(r, sentinel));
            ArrayDeque<Dependency> path = new ArrayDeque<>();
            collectResult.getRoot().accept(new TreeDependencyVisitor(new DependencyVisitor() {
                @Override
                public boolean visitEnter(DependencyNode node) {
                    Dependency parent = path.peek() == null ? sentinel : path.peek();
                    node.getRepositories().forEach(r -> repositories.putIfAbsent(r, parent));
                    path.push(node.getDependency() != null ? node.getDependency() : sentinel);
                    return true;
                }

                @Override
                public boolean visitLeave(DependencyNode node) {
                    path.pop();
                    return true;
                }
            }));
            if (repositories.isEmpty()) {
                output.info("No remote repository is used by {} {}.", contextName, resolutionRoot.getArtifact());
                result.put(contextName, Collections.emptyList());
                continue;
            }

            output.info("Remote repositories used by {} {}.", contextName, resolutionRoot.getArtifact());
            Map<Boolean, List<RemoteRepository>> repoGroupByMirrors = repositories.keySet().stream()
                    .collect(Collectors.groupingBy(
                            repo -> repo.getMirroredRepositories().isEmpty()));
            repoGroupByMirrors
                    .getOrDefault(Boolean.TRUE, Collections.emptyList())
                    .forEach(r -> {
                        output.info(" * {}", r);
                        Dependency firstIntroduced = repositories.get(r);
                        output.info(
                                "   First introduced on {}", firstIntroduced == sentinel ? "root" : firstIntroduced);
                    });

            Map<RemoteRepository, RemoteRepository> mirrorMap = new HashMap<>();
            repoGroupByMirrors
                    .getOrDefault(Boolean.FALSE, Collections.emptyList())
                    .forEach(repo -> repo.getMirroredRepositories().forEach(mrepo -> mirrorMap.put(mrepo, repo)));
            mirrorMap.forEach((r, mirror) -> {
                output.info(" * {}", r);
                Dependency firstIntroduced = repositories.get(mirror);
                output.info("   First introduced on {}", firstIntroduced == sentinel ? "root" : firstIntroduced);
                output.info("   Mirrored by {}", mirror);
            });
            result.put(contextName, List.copyOf(repositories.keySet()));
        }
        return Result.success(result);
    }

    @Override
    public Result<List<Artifact>> listAvailablePlugins(Collection<String> groupIds, Logger output) throws Exception {
        output.debug("Listing plugins in groupIds: {}", groupIds);
        List<Artifact> plugins = toolboxResolver.listAvailablePlugins(groupIds);
        plugins.forEach(p -> output.info(p.toString()));
        return plugins.isEmpty() ? Result.failure("No plugins") : Result.success(plugins);
    }

    @Override
    public Result<String> recordStart(Logger output) {
        boolean result = artifactRecorder.setActive(true);
        if (result) {
            artifactRecorder.clear();
            output.info("Started recorder...");
        } else {
            output.info("Recorder was already started.");
        }
        return result ? Result.success("Started") : Result.failure("No recorder state changed");
    }

    @Override
    public Result<RecordStats> recordStats(Logger output) {
        boolean active = artifactRecorder.isActive();
        int recordedCount = artifactRecorder.recordedCount();
        output.info("Recorder is {}; recorded {} artifacts so far", active ? "started" : "stopped", recordedCount);
        return Result.success(new RecordStats(active, recordedCount));
    }

    @Override
    public Result<String> recordStop(Logger output) {
        boolean result = artifactRecorder.setActive(false);
        if (result) {
            output.info("Stopped recorder, recorded {} artifacts", artifactRecorder.recordedCount());
        } else {
            output.info("Recorder was not started.");
        }
        return result ? Result.success("Stopped") : Result.failure("No recorder state changed");
    }

    @Override
    public Result<List<Artifact>> resolve(
            Source<Artifact> artifactSource,
            boolean sources,
            boolean javadoc,
            boolean signature,
            Sink<Artifact> sink,
            Logger output)
            throws Exception {
        List<Artifact> result = new ArrayList<>();
        List<Artifact> artifacts = artifactSource.get().toList();
        output.debug("Resolving {}", artifacts);
        try (Sink<Artifact> artifactSink =
                ArtifactSinks.teeArtifactSink(sink, ArtifactSinks.statArtifactSink(0, true, logger))) {
            List<Artifact> artifactResults = toolboxResolver.resolveArtifacts(artifacts).stream()
                    .map(ArtifactResult::getArtifact)
                    .toList();
            result.addAll(artifactResults);
            artifactSink.accept(artifactResults);
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
                    output.debug("Resolving (best effort) {}", subartifacts);
                    try {
                        List<Artifact> subartifactResults = toolboxResolver.resolveArtifacts(subartifacts).stream()
                                .map(ArtifactResult::getArtifact)
                                .toList();
                        result.addAll(subartifactResults);
                        artifactSink.accept(subartifactResults);
                    } catch (ArtifactResolutionException e) {
                        // ignore, this is "best effort"
                        List<Artifact> bestEffort = e.getResults().stream()
                                .filter(ArtifactResult::isResolved)
                                .map(ArtifactResult::getArtifact)
                                .toList();
                        result.addAll(bestEffort);
                        artifactSink.accept(bestEffort);
                    }
                }
            }
            return result.isEmpty() ? Result.failure("No artifacts") : Result.success(result);
        }
    }

    @Override
    public Result<List<Artifact>> resolveTransitive(
            ResolutionScope resolutionScope,
            Collection<ResolutionRoot> resolutionRoots,
            boolean sources,
            boolean javadoc,
            boolean signature,
            Sink<Artifact> sink,
            Logger output)
            throws Exception {
        ArtifactSinks.StatArtifactSink stat = ArtifactSinks.statArtifactSink(0, false, logger);
        try (Sink<Artifact> artifactSink = ArtifactSinks.teeArtifactSink(sink, stat)) {
            for (ResolutionRoot resolutionRoot : resolutionRoots) {
                doResolveTransitive(
                        resolutionScope,
                        resolutionRoot,
                        sources,
                        javadoc,
                        signature,
                        ArtifactSinks.teeArtifactSink(
                                ArtifactSinks.nonClosingArtifactSink(artifactSink),
                                ArtifactSinks.statArtifactSink(1, true, logger)),
                        output);
            }
        }
        return stat.getSeenArtifacts().isEmpty()
                ? Result.failure("No artifacts")
                : Result.success(stat.getSeenArtifacts());
    }

    private void doResolveTransitive(
            ResolutionScope resolutionScope,
            ResolutionRoot resolutionRoot,
            boolean sources,
            boolean javadoc,
            boolean signature,
            Sink<Artifact> sink,
            Logger output)
            throws Exception {
        try (Sink<Artifact> artifactSink = sink) {
            output.debug("Resolving {}", resolutionRoot.getArtifact());
            resolutionRoot = toolboxResolver.loadRoot(resolutionRoot);
            DependencyResult dependencyResult = toolboxResolver.resolve(
                    resolutionScope,
                    resolutionRoot.getArtifact(),
                    resolutionRoot.getDependencies(),
                    resolutionRoot.getManagedDependencies());
            List<ArtifactResult> adjustedResults = resolutionRoot.isLoad()
                    ? dependencyResult.getArtifactResults()
                    : (dependencyResult.getArtifactResults().size() == 1
                            ? Collections.emptyList()
                            : dependencyResult
                                    .getArtifactResults()
                                    .subList(
                                            1,
                                            dependencyResult
                                                            .getArtifactResults()
                                                            .size()
                                                    - 1));
            artifactSink.accept(
                    adjustedResults.stream().map(ArtifactResult::getArtifact).collect(Collectors.toList()));

            if (sources || javadoc || signature) {
                HashSet<Artifact> subartifacts = new HashSet<>();
                adjustedResults.stream().map(ArtifactResult::getArtifact).forEach(a -> {
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
                    output.debug("Resolving (best effort) {}", subartifacts);
                    try {
                        List<ArtifactResult> subartifactResults = toolboxResolver.resolveArtifacts(subartifacts);
                        artifactSink.accept(subartifactResults.stream()
                                .map(ArtifactResult::getArtifact)
                                .collect(Collectors.toList()));
                    } catch (ArtifactResolutionException e) {
                        // ignore, this is "best effort"
                        artifactSink.accept(e.getResults().stream()
                                .filter(ArtifactResult::isResolved)
                                .map(ArtifactResult::getArtifact)
                                .collect(Collectors.toList()));
                    }
                }
            }
        }
    }

    @Override
    public Result<Node> tree(
            ResolutionScope resolutionScope, ResolutionRoot resolutionRoot, boolean verboseTree, Logger output)
            throws Exception {
        output.debug("Loading root of: {}", resolutionRoot.getArtifact());
        ResolutionRoot root = toolboxResolver.loadRoot(resolutionRoot);
        output.debug("Collecting graph of: {}", resolutionRoot.getArtifact());
        CollectResult collectResult = toolboxResolver.collect(
                resolutionScope,
                root.getArtifact(),
                root.getDependencies(),
                root.getManagedDependencies(),
                verboseTree);
        collectResult.getRoot().accept(new DependencyGraphDumper(output::info));
        // TODO: implement this
        return Result.success(new Node(collectResult.getRoot().getArtifact(), "", Collections.emptyList()));
    }

    @Override
    public Result<List<List<Artifact>>> treeFind(
            ResolutionScope resolutionScope,
            ResolutionRoot resolutionRoot,
            boolean verboseTree,
            ArtifactMatcher artifactMatcher,
            Logger output)
            throws Exception {
        output.debug("Loading root of: {}", resolutionRoot.getArtifact());
        ResolutionRoot root = toolboxResolver.loadRoot(resolutionRoot);
        output.debug("Collecting graph of: {}", resolutionRoot.getArtifact());
        CollectResult collectResult = toolboxResolver.collect(
                resolutionScope,
                root.getArtifact(),
                root.getDependencies(),
                root.getManagedDependencies(),
                verboseTree);
        PathRecordingDependencyVisitor pathRecordingDependencyVisitor = new PathRecordingDependencyVisitor(
                (node, parents) -> node.getArtifact() != null && artifactMatcher.test(node.getArtifact()));
        collectResult.getRoot().accept(pathRecordingDependencyVisitor);
        List<List<Artifact>> result = new ArrayList<>();
        if (!pathRecordingDependencyVisitor.getPaths().isEmpty()) {
            output.info("Paths");
            for (List<DependencyNode> path : pathRecordingDependencyVisitor.getPaths()) {
                result.add(path.stream().map(DependencyNode::getArtifact).toList());
                String indent = "";
                for (DependencyNode node : path) {
                    output.info("{}-> {}", indent, node.getArtifact());
                    indent += "  ";
                }
            }
        } else {
            output.info("No paths found.");
        }
        return Result.success(result);
    }

    @Override
    public Result<List<Dependency>> dmList(ResolutionRoot resolutionRoot, boolean verboseList, Logger output)
            throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        List<Dependency> result = toolboxResolver.loadRoot(resolutionRoot).getManagedDependencies();
        result.forEach(d -> output.info(
                " {}. {}", counter.incrementAndGet(), d.getScope().trim().isEmpty() ? d.getArtifact() : d));
        return Result.success(result);
    }

    @Override
    public Result<Node> dmTree(ResolutionRoot resolutionRoot, boolean verboseTree, Logger output) throws Exception {
        resolutionRoot = toolboxResolver.loadRoot(resolutionRoot);
        CollectResult collectResult = toolboxResolver.collectDm(
                resolutionRoot.getArtifact(), resolutionRoot.getManagedDependencies(), verboseTree);
        collectResult.getRoot().accept(new DependencyGraphDumper(output::info));
        // TODO: implement this
        return Result.success(new Node(collectResult.getRoot().getArtifact(), "", Collections.emptyList()));
    }

    @Override
    public Map<String, RemoteRepository> getKnownSearchRemoteRepositories() {
        return knownSearchRemoteRepositories;
    }

    @Override
    public Result<Map<Artifact, Boolean>> exists(
            RemoteRepository remoteRepository,
            String gav,
            boolean pom,
            boolean sources,
            boolean javadoc,
            boolean signature,
            boolean allRequired,
            String repositoryVendor,
            Logger output)
            throws IOException {
        HashMap<Artifact, Boolean> result = new HashMap<>();
        ArrayList<Artifact> missingOnes = new ArrayList<>();
        ArrayList<Artifact> existingOnes = new ArrayList<>();
        try (SearchBackend backend = toolboxSearchApi.getRemoteRepositoryBackend(
                context.repositorySystemSession(), remoteRepository, repositoryVendor)) {
            Artifact artifact = new DefaultArtifact(gav);
            boolean exists = toolboxSearchApi.exists(backend, artifact);
            result.put(artifact, exists);
            if (!exists) {
                missingOnes.add(artifact);
            } else {
                existingOnes.add(artifact);
            }
            output.info("Artifact {} {}", artifact, exists ? "EXISTS" : "NOT EXISTS");
            if (pom && !"pom".equals(artifact.getExtension())) {
                Artifact poma = new SubArtifact(artifact, null, "pom");
                exists = toolboxSearchApi.exists(backend, poma);
                result.put(poma, exists);
                if (!exists && allRequired) {
                    missingOnes.add(poma);
                } else if (allRequired) {
                    existingOnes.add(poma);
                }
                output.info("    {} {}", poma, exists ? "EXISTS" : "NOT EXISTS");
            }
            if (sources) {
                Artifact sourcesa = new SubArtifact(artifact, "sources", "jar");
                exists = toolboxSearchApi.exists(backend, sourcesa);
                result.put(sourcesa, exists);
                if (!exists && allRequired) {
                    missingOnes.add(sourcesa);
                } else if (allRequired) {
                    existingOnes.add(sourcesa);
                }
                output.info("    {} {}", sourcesa, exists ? "EXISTS" : "NOT EXISTS");
            }
            if (javadoc) {
                Artifact javadoca = new SubArtifact(artifact, "javadoc", "jar");
                exists = toolboxSearchApi.exists(backend, javadoca);
                result.put(javadoca, exists);
                if (!exists && allRequired) {
                    missingOnes.add(javadoca);
                } else if (allRequired) {
                    existingOnes.add(javadoca);
                }
                output.info("    {} {}", javadoca, exists ? "EXISTS" : "NOT EXISTS");
            }
            if (signature) {
                Artifact signaturea = new SubArtifact(artifact, null, artifact.getExtension() + ".asc");
                exists = toolboxSearchApi.exists(backend, signaturea);
                result.put(signaturea, exists);
                if (!exists && allRequired) {
                    missingOnes.add(signaturea);
                } else if (allRequired) {
                    existingOnes.add(signaturea);
                }
                output.info("    {} {}", signaturea, exists ? "EXISTS" : "NOT EXISTS");
            }
        }
        output.info("");
        output.info(
                "Checked TOTAL of {} (existing: {} not existing: {})",
                existingOnes.size() + missingOnes.size(),
                existingOnes.size(),
                missingOnes.size());
        return missingOnes.isEmpty() ? Result.success(result) : Result.failure("Missing artifacts");
    }

    @Override
    public Result<Map<String, Artifact>> identify(
            RemoteRepository remoteRepository, Collection<String> targets, boolean decorated, Logger output)
            throws IOException {
        HashMap<String, Artifact> result = new HashMap<>();
        HashMap<String, String> sha1s = new HashMap<>();
        for (String target : targets) {
            if (Files.exists(Paths.get(target))) {
                try {
                    output.info("Calculating SHA1 of file {}", target);
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
                    sha1s.put(target, ChecksumUtils.toHexString(sha1md.digest()));
                } catch (NoSuchAlgorithmException e) {
                    throw new IllegalStateException("SHA1 MessageDigest unavailable", e);
                }
            } else {
                sha1s.put(target, target);
            }
        }
        BiConsumer<Map.Entry<String, String>, Artifact> render = (e, a) -> {
            String hit = a != null ? a.toString() : "?";
            if (decorated) {
                if (!Objects.equals(e.getKey(), e.getValue())) {
                    output.info("{} ({}) = {}", e.getValue(), e.getKey(), hit);
                } else {
                    output.info("{} = {}", e.getValue(), hit);
                }
            } else {
                output.info(hit);
            }
        };
        int hits = 0;
        for (Map.Entry<String, String> sha1 : sha1s.entrySet()) {
            output.info("Identifying artifact with SHA1={}", sha1.getValue());
            try (SearchBackend backend =
                    toolboxSearchApi.getSmoBackend(context.repositorySystemSession(), remoteRepository)) {
                SearchRequest searchRequest = new SearchRequest(fieldQuery(MAVEN.SHA1, sha1.getValue()));
                SearchResponse searchResponse = backend.search(searchRequest);
                if (searchResponse.getCurrentHits() == 0) {
                    result.put(sha1.getKey(), null);
                    render.accept(sha1, null);
                } else {
                    while (searchResponse.getCurrentHits() > 0) {
                        Collection<Artifact> res = toolboxSearchApi.renderArtifacts(
                                context.repositorySystemSession(), searchResponse.getPage(), null);
                        if (res.isEmpty()) {
                            result.put(sha1.getKey(), null);
                            render.accept(sha1, null);
                        } else {
                            for (Artifact artifact : res) {
                                result.put(sha1.getKey(), artifact);
                                render.accept(sha1, artifact);
                                hits++;
                            }
                        }

                        searchResponse =
                                backend.search(searchResponse.getSearchRequest().nextPage());
                    }
                }
            }
        }
        return result.isEmpty() ? Result.failure("No matches") : Result.success(result);
    }

    @Override
    public Result<List<String>> list(
            RemoteRepository remoteRepository, String gavoid, String repositoryVendor, Logger output)
            throws IOException {
        ArrayList<String> result = new ArrayList<>();
        try (SearchBackend backend = toolboxSearchApi.getRemoteRepositoryBackend(
                context.repositorySystemSession(), remoteRepository, repositoryVendor)) {
            String[] elements = gavoid.split(":");
            if (elements.length < 1 || elements.length > 3) {
                throw new IllegalArgumentException("Invalid gavoid");
            }

            Query query = fieldQuery(MAVEN.GROUP_ID, elements[0]);
            if (elements.length > 1) {
                query = and(query, fieldQuery(MAVEN.ARTIFACT_ID, elements[1]));
            }

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

            Collection<String> gavoids = toolboxSearchApi.renderGavoid(searchResponse.getPage(), versionPredicate);
            for (String g : gavoids) {
                result.add(g);
                output.info(g);
            }
        }
        return Result.success(result);
    }

    @Override
    public Result<List<Artifact>> search(RemoteRepository remoteRepository, String expression, Logger output)
            throws IOException {
        ArrayList<Artifact> result = new ArrayList<>();
        try (SearchBackend backend =
                toolboxSearchApi.getSmoBackend(context.repositorySystemSession(), remoteRepository)) {
            Query query;
            try {
                query = toolboxSearchApi.toSmoQuery(new DefaultArtifact(expression));
            } catch (IllegalArgumentException e) {
                query = query(expression);
            }
            SearchRequest searchRequest = new SearchRequest(query);
            SearchResponse searchResponse = backend.search(searchRequest);

            Collection<Artifact> artifacts =
                    toolboxSearchApi.renderArtifacts(session(), searchResponse.getPage(), null);
            for (Artifact artifact : artifacts) {
                result.add(artifact);
                output.info(artifact.toString());
                if (output.isDebugEnabled()) {
                    output.debug(artifact.getProperties().toString());
                }
            }
            while (searchResponse.getCurrentHits() > 0) {
                searchResponse =
                        backend.search(searchResponse.getSearchRequest().nextPage());
                artifacts = toolboxSearchApi.renderArtifacts(session(), searchResponse.getPage(), null);
                for (Artifact artifact : artifacts) {
                    result.add(artifact);
                    output.info(artifact.toString());
                    if (output.isDebugEnabled()) {
                        output.debug(artifact.getProperties().toString());
                    }
                }
            }
        }
        return Result.success(result);
    }

    @Override
    public Result<Boolean> verify(
            RemoteRepository remoteRepository, String gav, String sha1, String repositoryVendor, Logger output)
            throws IOException {
        try (SearchBackend backend = toolboxSearchApi.getRemoteRepositoryBackend(
                context.repositorySystemSession(), remoteRepository, repositoryVendor)) {
            Artifact artifact = new DefaultArtifact(gav);
            boolean verified = toolboxSearchApi.verify(backend, new DefaultArtifact(gav), sha1);
            output.info("Artifact SHA1({})={}: {}", artifact, sha1, verified ? "MATCHED" : "NOT MATCHED");
            return Result.success(verified);
        }
    }

    // Various

    @Override
    public Result<Float> libYear(
            String subject,
            ResolutionScope resolutionScope,
            ResolutionRoot resolutionRoot,
            boolean transitive,
            boolean quiet,
            boolean upToDate,
            Predicate<Version> versionPredicate,
            BiFunction<Artifact, List<Version>, String> artifactVersionSelector,
            String repositoryVendor,
            Logger output)
            throws Exception {
        ArrayList<SearchBackend> searchBackends = new ArrayList<>();
        for (RemoteRepository remoteRepository : context.remoteRepositories()) {
            searchBackends.add(toolboxSearchApi.getRemoteRepositoryBackend(
                    context.repositorySystemSession(), remoteRepository, repositoryVendor));
        }

        LibYearSink sink = LibYearSink.libYear(
                logger,
                subject,
                context,
                toolboxResolver,
                toolboxSearchApi,
                quiet,
                upToDate,
                versionPredicate,
                artifactVersionSelector,
                searchBackends);
        try (sink) {
            try {
                ArrayList<Artifact> artifacts = new ArrayList<>();
                ResolutionRoot root = toolboxResolver.loadRoot(resolutionRoot);
                if (transitive) {
                    CollectResult collectResult = toolboxResolver.collect(
                            resolutionScope,
                            root.getArtifact(),
                            root.getDependencies(),
                            root.getManagedDependencies(),
                            false);
                    collectResult.getRoot().accept(new DependencyVisitor() {
                        @Override
                        public boolean visitEnter(DependencyNode node) {
                            artifacts.add(node.getArtifact());
                            return true;
                        }

                        @Override
                        public boolean visitLeave(DependencyNode node) {
                            return true;
                        }
                    });
                } else {
                    artifacts.addAll(resolutionRoot.getDependencies().stream()
                            .map(this::toArtifact)
                            .toList());
                }
                sink.accept(artifacts);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return Result.success(sink.getTotalLibyear());
    }

    @Override
    public Result<Map<Artifact, List<Version>>> versions(
            String context, Source<Artifact> artifactSource, Predicate<Version> versionPredicate, Logger output)
            throws Exception {
        List<Artifact> artifacts = artifactSource.get().toList();
        HashMap<Artifact, List<Version>> result = new HashMap<>();
        output.info("Checking newest versions of {} ({})", context, artifacts.size());
        for (Artifact artifact : artifacts) {
            List<Version> newer = toolboxResolver.findNewerVersions(artifact, versionPredicate);
            result.put(artifact, newer);
            if (!newer.isEmpty()) {
                Version latest = newer.getLast();
                String all = newer.stream().map(Object::toString).collect(Collectors.joining(", "));
                output.info("* {} -> {}", ArtifactIdUtils.toId(artifact), latest);
                output.info("  Available: {}", all);
            } else {
                output.info("* {} is up to date", ArtifactIdUtils.toId(artifact));
            }
        }
        return Result.success(result);
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
}
