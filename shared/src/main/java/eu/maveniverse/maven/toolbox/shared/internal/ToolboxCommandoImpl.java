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
import eu.maveniverse.maven.toolbox.shared.ReactorLocator;
import eu.maveniverse.maven.toolbox.shared.ResolutionRoot;
import eu.maveniverse.maven.toolbox.shared.ResolutionScope;
import eu.maveniverse.maven.toolbox.shared.Result;
import eu.maveniverse.maven.toolbox.shared.Sink;
import eu.maveniverse.maven.toolbox.shared.Source;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import eu.maveniverse.maven.toolbox.shared.output.Output;
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

public class ToolboxCommandoImpl implements ToolboxCommando {
    private final Output output;
    private final Context context;
    private final RepositorySystemSession session;
    private final VersionScheme versionScheme;
    private final ToolboxSearchApiImpl toolboxSearchApi;
    private final ArtifactRecorderImpl artifactRecorder;
    private final ToolboxResolverImpl toolboxResolver;

    private final Map<String, RemoteRepository> knownSearchRemoteRepositories;

    public ToolboxCommandoImpl(Output output, Context context) {
        this.output = requireNonNull(output, "output");
        this.context = requireNonNull(context, "context");
        this.versionScheme = new GenericVersionScheme();
        this.toolboxSearchApi = new ToolboxSearchApiImpl(output);
        this.artifactRecorder = new ArtifactRecorderImpl();
        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession(context.repositorySystemSession());
        session.setRepositoryListener(
                ChainedRepositoryListener.newInstance(session.getRepositoryListener(), artifactRecorder));
        this.session = session;
        this.toolboxResolver = new ToolboxResolverImpl(
                output,
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

    public Output output() {
        return output;
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
    public Result<String> dump() {
        Runtime runtime = context.getRuntime();
        output.marker(Output.Verbosity.TIGHT)
                .emphasize("Toolbox {}")
                .normal(" (MIMA Runtime '{}' version {})")
                .say(getVersion(), runtime.name(), runtime.version());
        output.doTell("");
        output.tell("          Maven version {}", runtime.mavenVersion());
        output.tell("                Managed {}", runtime.managedRepositorySystem());
        output.tell("                Basedir {}", context.basedir());
        output.tell(
                "                Offline {}", context.repositorySystemSession().isOffline());

        MavenSystemHome mavenSystemHome = context.mavenSystemHome();
        output.tell("");
        output.tell("             MAVEN_HOME {}", mavenSystemHome == null ? "undefined" : mavenSystemHome.basedir());
        if (mavenSystemHome != null) {
            output.tell("           settings.xml {}", mavenSystemHome.settingsXml());
            output.tell("         toolchains.xml {}", mavenSystemHome.toolchainsXml());
        }

        MavenUserHome mavenUserHome = context.mavenUserHome();
        output.tell("");
        output.tell("              USER_HOME {}", mavenUserHome.basedir());
        output.tell("           settings.xml {}", mavenUserHome.settingsXml());
        output.tell("  settings-security.xml {}", mavenUserHome.settingsSecurityXml());
        output.tell("       local repository {}", mavenUserHome.localRepository());

        output.tell("");
        output.tell("               PROFILES");
        output.tell("                 Active {}", context.contextOverrides().getActiveProfileIds());
        output.tell("               Inactive {}", context.contextOverrides().getInactiveProfileIds());

        output.tell("");
        output.tell("    REMOTE REPOSITORIES");
        for (RemoteRepository repository : context.remoteRepositories()) {
            if (repository.getMirroredRepositories().isEmpty()) {
                output.tell("                        {}", repository);
            } else {
                output.tell("                        {}, mirror of", repository);
                for (RemoteRepository mirrored : repository.getMirroredRepositories()) {
                    output.tell("                          {}", mirrored);
                }
            }
        }

        if (context.httpProxy() != null) {
            HTTPProxy proxy = context.httpProxy();
            output.tell("");
            output.tell("             HTTP PROXY");
            output.tell("                    url {}://{}:{}", proxy.getProtocol(), proxy.getHost(), proxy.getPort());
            output.tell("          nonProxyHosts {}", proxy.getNonProxyHosts());
        }

        if (output.isHeard(Output.Verbosity.SUGGEST)) {
            output.suggest("");
            output.suggest("        USER PROPERTIES");
            context.repositorySystemSession().getUserProperties().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> output.suggest("                         {}={}", e.getKey(), e.getValue()));
            output.suggest("      SYSTEM PROPERTIES");
            context.repositorySystemSession().getSystemProperties().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> output.suggest("                         {}={}", e.getKey(), e.getValue()));
            output.suggest("      CONFIG PROPERTIES");
            context.repositorySystemSession().getConfigProperties().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> output.suggest("                         {}={}", e.getKey(), e.getValue()));
            output.suggest("");

            if (output.isHeard(Output.Verbosity.CHATTER)) {
                output.tell("OUTPUT TEST:");
                output.chatter("Chatter: {}", "Message", new RuntimeException("runtime"));
                output.suggest("Suggest: {}", "Message", new RuntimeException("runtime"));
                output.tell("Tell: {}", "Message", new RuntimeException("runtime"));
                output.doTell("Do Tell: {}", "Message", new RuntimeException("runtime"));
            }
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
            output.warn("Could not resolve artifact version: {}", dependency.getArtifact(), e);
            return dependency.getArtifact();
        }
    }

    @Override
    public Result<String> classpath(ResolutionScope resolutionScope, ResolutionRoot resolutionRoot) throws Exception {
        output.suggest("Resolving {}", resolutionRoot.getArtifact());
        resolutionRoot = toolboxResolver.loadRoot(resolutionRoot);
        DependencyResult dependencyResult = toolboxResolver.resolve(
                resolutionScope,
                resolutionRoot.getArtifact(),
                resolutionRoot.getDependencies(),
                resolutionRoot.getManagedDependencies());

        PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
        dependencyResult.getRoot().accept(nlg);
        String classpath = nlg.getClassPath();
        output.doTell(classpath);
        if (nlg.getFiles().isEmpty()) {
            return Result.failure("No files");
        } else {
            return Result.success(classpath);
        }
    }

    @Override
    public Result<List<Artifact>> copy(Source<Artifact> source, Sink<Artifact> sink) throws Exception {
        try (source;
                sink) {
            List<Artifact> resolveResult = toolboxResolver
                    .resolveArtifacts(source.get().toList())
                    .stream()
                    .filter(ArtifactResult::isResolved)
                    .map(ArtifactResult::getArtifact)
                    .toList();
            sink.accept(resolveResult);
            output.tell("Resolved {} artifacts", resolveResult.size());
            return resolveResult.isEmpty() ? Result.failure("No artifacts") : Result.success(resolveResult);
        }
    }

    @Override
    public Result<List<Artifact>> copyTransitive(
            ResolutionScope resolutionScope, Collection<ResolutionRoot> resolutionRoots, Sink<Artifact> sink)
            throws Exception {
        ArrayList<Artifact> artifactResults = new ArrayList<>();
        for (ResolutionRoot resolutionRoot : resolutionRoots) {
            output.suggest("Resolving {}", resolutionRoot.getArtifact());
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
        return copy(artifactResults::stream, sink);
    }

    @Override
    public Result<List<Artifact>> copyRecorded(boolean stopRecording, Sink<Artifact> sink) throws Exception {
        artifactRecorder.setActive(!stopRecording);
        return copy(artifactRecorder, sink);
    }

    @Override
    public Result<Map<String, List<RemoteRepository>>> listRepositories(
            ResolutionScope resolutionScope, Map<String, ResolutionRoot> resolutionRoots) throws Exception {
        HashMap<String, List<RemoteRepository>> result = new HashMap<>();
        for (Map.Entry<String, ResolutionRoot> entry : resolutionRoots.entrySet()) {
            String contextName = entry.getKey();
            ResolutionRoot resolutionRoot = entry.getValue();
            output.chatter("Loading root of {} {}", contextName, resolutionRoot.getArtifact());
            ResolutionRoot root = toolboxResolver.loadRoot(resolutionRoot);
            output.chatter("Collecting graph of: {}", resolutionRoot.getArtifact());
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
                output.tell("No remote repository is used by {} {}.", contextName, resolutionRoot.getArtifact());
                result.put(contextName, Collections.emptyList());
                continue;
            }

            output.tell("Remote repositories used by {} {}.", contextName, resolutionRoot.getArtifact());
            Map<Boolean, List<RemoteRepository>> repoGroupByMirrors = repositories.keySet().stream()
                    .collect(Collectors.groupingBy(
                            repo -> repo.getMirroredRepositories().isEmpty()));
            repoGroupByMirrors
                    .getOrDefault(Boolean.TRUE, Collections.emptyList())
                    .forEach(r -> {
                        output.tell(" * {}", r);
                        Dependency firstIntroduced = repositories.get(r);
                        output.tell(
                                "   First introduced on {}", firstIntroduced == sentinel ? "root" : firstIntroduced);
                    });

            Map<RemoteRepository, RemoteRepository> mirrorMap = new HashMap<>();
            repoGroupByMirrors
                    .getOrDefault(Boolean.FALSE, Collections.emptyList())
                    .forEach(repo -> repo.getMirroredRepositories().forEach(mrepo -> mirrorMap.put(mrepo, repo)));
            mirrorMap.forEach((r, mirror) -> {
                output.tell(" * {}", r);
                Dependency firstIntroduced = repositories.get(mirror);
                output.tell("   First introduced on {}", firstIntroduced == sentinel ? "root" : firstIntroduced);
                output.tell("   Mirrored by {}", mirror);
            });
            result.put(contextName, List.copyOf(repositories.keySet()));
        }
        return Result.success(result);
    }

    @Override
    public Result<List<Artifact>> listAvailablePlugins(Collection<String> groupIds) throws Exception {
        output.suggest("Listing plugins in groupIds: {}", groupIds);
        List<Artifact> plugins = toolboxResolver.listAvailablePlugins(groupIds);
        plugins.forEach(p -> output.tell(p.toString()));
        return plugins.isEmpty() ? Result.failure("No plugins") : Result.success(plugins);
    }

    @Override
    public Result<String> recordStart() {
        boolean result = artifactRecorder.setActive(true);
        if (result) {
            artifactRecorder.clear();
            output.tell("Started recorder...");
        } else {
            output.tell("Recorder was already started.");
        }
        return result ? Result.success("Started") : Result.failure("No recorder state changed");
    }

    @Override
    public Result<RecordStats> recordStats() {
        boolean active = artifactRecorder.isActive();
        int recordedCount = artifactRecorder.recordedCount();
        output.tell("Recorder is {}; recorded {} artifacts so far", active ? "started" : "stopped", recordedCount);
        return Result.success(new RecordStats(active, recordedCount));
    }

    @Override
    public Result<String> recordStop() {
        boolean result = artifactRecorder.setActive(false);
        if (result) {
            output.tell("Stopped recorder, recorded {} artifacts", artifactRecorder.recordedCount());
        } else {
            output.tell("Recorder was not started.");
        }
        return result ? Result.success("Stopped") : Result.failure("No recorder state changed");
    }

    @Override
    public Result<List<Artifact>> resolve(
            Source<Artifact> artifactSource, boolean sources, boolean javadoc, boolean signature, Sink<Artifact> sink)
            throws Exception {
        List<Artifact> result = new ArrayList<>();
        List<Artifact> artifacts = artifactSource.get().toList();
        output.suggest("Resolving {}", artifacts);
        try (Sink<Artifact> artifactSink =
                ArtifactSinks.teeArtifactSink(sink, ArtifactSinks.statArtifactSink(0, true, output))) {
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
                    output.suggest("Resolving (best effort) {}", subartifacts);
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
            Sink<Artifact> sink)
            throws Exception {
        ArtifactSinks.StatArtifactSink stat = ArtifactSinks.statArtifactSink(0, false, output);
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
                                ArtifactSinks.statArtifactSink(1, true, output)));
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
            Sink<Artifact> sink)
            throws Exception {
        try (Sink<Artifact> artifactSink = sink) {
            output.suggest("Resolving {}", resolutionRoot.getArtifact());
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
                    output.suggest("Resolving (best effort) {}", subartifacts);
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
    public Result<CollectResult> tree(
            ResolutionScope resolutionScope, ResolutionRoot resolutionRoot, boolean verboseTree) throws Exception {
        output.suggest("Loading root of: {}", resolutionRoot.getArtifact());
        ResolutionRoot root = toolboxResolver.loadRoot(resolutionRoot);
        output.suggest("Collecting graph of: {}", resolutionRoot.getArtifact());
        CollectResult collectResult = toolboxResolver.collect(
                resolutionScope,
                root.getArtifact(),
                root.getDependencies(),
                root.getManagedDependencies(),
                verboseTree);
        collectResult
                .getRoot()
                .accept(new DependencyGraphDumper(
                        output::tell,
                        DependencyGraphDumper.defaultsWith(DependencyGraphDumper.premanagedProperties()),
                        output.tool(
                                DependencyGraphDumper.LineFormatter.class, DependencyGraphDumper.LineFormatter::new)));
        return Result.success(collectResult);
    }

    @Override
    public Result<List<List<Artifact>>> treeFind(
            ResolutionScope resolutionScope,
            ResolutionRoot resolutionRoot,
            boolean verboseTree,
            ArtifactMatcher artifactMatcher)
            throws Exception {
        output.suggest("Loading root of: {}", resolutionRoot.getArtifact());
        ResolutionRoot root = toolboxResolver.loadRoot(resolutionRoot);
        output.suggest("Collecting graph of: {}", resolutionRoot.getArtifact());
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
            output.tell("Paths");
            for (List<DependencyNode> path : pathRecordingDependencyVisitor.getPaths()) {
                result.add(path.stream().map(DependencyNode::getArtifact).toList());
                String indent = "";
                for (DependencyNode node : path) {
                    output.tell("{}-> {}", indent, node.getArtifact());
                    indent += "  ";
                }
            }
        } else {
            output.tell("No paths found.");
        }
        return Result.success(result);
    }

    @Override
    public Result<List<Dependency>> dmList(ResolutionRoot resolutionRoot, boolean verboseList) throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        List<Dependency> result = toolboxResolver.loadRoot(resolutionRoot).getManagedDependencies();
        result.forEach(d -> output.tell(
                " {}. {}", counter.incrementAndGet(), d.getScope().trim().isEmpty() ? d.getArtifact() : d));
        return Result.success(result);
    }

    @Override
    public Result<CollectResult> dmTree(ResolutionRoot resolutionRoot, boolean verboseTree) throws Exception {
        resolutionRoot = toolboxResolver.loadRoot(resolutionRoot);
        CollectResult collectResult = toolboxResolver.collectDm(
                resolutionRoot.getArtifact(), resolutionRoot.getManagedDependencies(), verboseTree);
        collectResult
                .getRoot()
                .accept(new DependencyGraphDumper(
                        output::tell,
                        DependencyGraphDumper.defaultsWith(DependencyGraphDumper.premanagedProperties()),
                        output.tool(
                                DependencyGraphDumper.LineFormatter.class, DependencyGraphDumper.LineFormatter::new)));
        return Result.success(collectResult);
    }

    @Override
    public Result<CollectResult> projectInheritanceTree(ReactorLocator reactorLocator) {
        CollectResult collectResult = toolboxResolver.collectProjectInheritance(reactorLocator);
        collectResult
                .getRoot()
                .accept(new DependencyGraphDumper(
                        output::tell,
                        DependencyGraphDumper.defaultsWith(DependencyGraphDumper.artifactProperties(List.of("source"))),
                        output.tool(
                                DependencyGraphDumper.LineFormatter.class, DependencyGraphDumper.LineFormatter::new)));
        return Result.success(collectResult);
    }

    @Override
    public Result<CollectResult> projectCollectTree(ReactorLocator reactorLocator) throws Exception {
        CollectResult collectResult = toolboxResolver.collectProjectCollect(reactorLocator);
        collectResult
                .getRoot()
                .accept(new DependencyGraphDumper(
                        output::tell,
                        DependencyGraphDumper.defaultsWith(DependencyGraphDumper.artifactProperties(List.of("source"))),
                        output.tool(
                                DependencyGraphDumper.LineFormatter.class, DependencyGraphDumper.LineFormatter::new)));
        return Result.success(collectResult);
    }

    @Override
    public Result<CollectResult> projectModuleDependencyTree(
            ReactorLocator reactorLocator, ResolutionScope scope, boolean showExternal) {
        CollectResult collectResult =
                toolboxResolver.collectProjectModuleDependency(reactorLocator, scope, showExternal);
        collectResult
                .getRoot()
                .accept(new DependencyGraphDumper(
                        output::tell,
                        DependencyGraphDumper.defaultsWith(DependencyGraphDumper.artifactProperties(List.of("source"))),
                        output.tool(
                                DependencyGraphDumper.LineFormatter.class, DependencyGraphDumper.LineFormatter::new)));
        return Result.success(collectResult);
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
            String repositoryVendor)
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

            BiConsumer<Artifact, Boolean> reporter = (a, e) -> {
                if (e) {
                    output.marker(Output.Verbosity.NORMAL)
                            .normal("Artifact {} ")
                            .outstanding("EXISTS")
                            .say(a);
                } else {
                    output.marker(Output.Verbosity.NORMAL)
                            .normal("Artifact {} ")
                            .scary("NOT EXISTS")
                            .say(a);
                }
            };

            reporter.accept(artifact, exists);
            if (pom && !"pom".equals(artifact.getExtension())) {
                Artifact poma = new SubArtifact(artifact, null, "pom");
                exists = toolboxSearchApi.exists(backend, poma);
                result.put(poma, exists);
                if (!exists && allRequired) {
                    missingOnes.add(poma);
                } else if (allRequired) {
                    existingOnes.add(poma);
                }
                reporter.accept(poma, exists);
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
                reporter.accept(sourcesa, exists);
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
                reporter.accept(javadoca, exists);
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
                reporter.accept(signaturea, exists);
            }
        }
        output.tell("");
        output.marker(Output.Verbosity.TIGHT)
                .emphasize("Checked TOTAL of {} (existing: {} not existing: {})")
                .say(existingOnes.size() + missingOnes.size(), existingOnes.size(), missingOnes.size());
        return missingOnes.isEmpty() ? Result.success(result) : Result.failure("Missing artifacts");
    }

    @Override
    public Result<Map<String, Artifact>> identify(
            RemoteRepository remoteRepository, Collection<String> targets, boolean decorated) throws IOException {
        HashMap<String, String> sha1s = new HashMap<>();
        for (String target : targets) {
            if (Files.exists(Paths.get(target))) {
                try {
                    output.tell("Calculating SHA1 of file {}", target);
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

        Map<String, Artifact> result;
        try (SearchBackend backend =
                toolboxSearchApi.getSmoBackend(context.repositorySystemSession(), remoteRepository)) {
            result = toolboxSearchApi.identify(session(), backend, sha1s.values());
        }

        sha1s.forEach((key, value) -> {
            Artifact a = result.get(value);
            String hit = a != null ? a.toString() : "UNKNOWN";
            if (decorated) {
                if (!Objects.equals(key, value)) {
                    output.marker(Output.Verbosity.TIGHT)
                            .outstanding("{} ({}) = {}")
                            .say(value, key, hit);
                } else {
                    output.marker(Output.Verbosity.TIGHT).outstanding("{} = {}").say(value, hit);
                }
            } else {
                output.marker(Output.Verbosity.TIGHT).outstanding(hit).say();
            }
        });

        return result.isEmpty() ? Result.failure("No matches") : Result.success(result);
    }

    @Override
    public Result<List<String>> list(RemoteRepository remoteRepository, String gavoid, String repositoryVendor)
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
                output.tell(g);
            }
        }
        return Result.success(result);
    }

    @Override
    public Result<List<Artifact>> search(RemoteRepository remoteRepository, String expression) throws IOException {
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
                output.tell(artifact.toString());
                output.suggest(artifact.getProperties().toString());
            }
            while (searchResponse.getCurrentHits() > 0) {
                searchResponse =
                        backend.search(searchResponse.getSearchRequest().nextPage());
                artifacts = toolboxSearchApi.renderArtifacts(session(), searchResponse.getPage(), null);
                for (Artifact artifact : artifacts) {
                    result.add(artifact);
                    output.tell(artifact.toString());
                    output.suggest(artifact.getProperties().toString());
                }
            }
        }
        return Result.success(result);
    }

    @Override
    public Result<Boolean> verify(RemoteRepository remoteRepository, String gav, String sha1, String repositoryVendor)
            throws IOException {
        try (SearchBackend backend = toolboxSearchApi.getRemoteRepositoryBackend(
                context.repositorySystemSession(), remoteRepository, repositoryVendor)) {
            Artifact artifact = new DefaultArtifact(gav);
            boolean verified = toolboxSearchApi.verify(backend, new DefaultArtifact(gav), sha1);
            output.tell("Artifact SHA1({})={}: {}", artifact, sha1, verified ? "MATCHED" : "NOT MATCHED");
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
            boolean upToDate,
            Predicate<Version> versionPredicate,
            BiFunction<Artifact, List<Version>, String> artifactVersionSelector,
            String repositoryVendor)
            throws Exception {
        ArrayList<SearchBackend> searchBackends = new ArrayList<>();
        for (RemoteRepository remoteRepository : context.remoteRepositories()) {
            searchBackends.add(toolboxSearchApi.getRemoteRepositoryBackend(
                    context.repositorySystemSession(), remoteRepository, repositoryVendor));
        }

        LibYearSink sink = LibYearSink.libYear(
                output,
                subject,
                context,
                toolboxResolver,
                toolboxSearchApi,
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
                            if (node != collectResult.getRoot()) {
                                artifacts.add(node.getArtifact());
                            }
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
            String context, Source<Artifact> artifactSource, Predicate<Version> versionPredicate) throws Exception {
        List<Artifact> artifacts = artifactSource.get().toList();
        HashMap<Artifact, List<Version>> result = new HashMap<>();
        output.marker(Output.Verbosity.NORMAL)
                .emphasize("Checking newest versions of {} ({})")
                .say(context, artifacts.size());
        for (Artifact artifact : artifacts) {
            List<Version> newer = toolboxResolver.findNewerVersions(artifact, versionPredicate);
            result.put(artifact, newer);
            if (!newer.isEmpty()) {
                Version latest = newer.getLast();
                String all = newer.stream().map(Object::toString).collect(Collectors.joining(", "));
                output.marker(Output.Verbosity.NORMAL).scary("* {} -> {}").say(ArtifactIdUtils.toId(artifact), latest);
                output.marker(Output.Verbosity.SUGGEST)
                        .detail("  Available: {}")
                        .say(all);
            } else {
                output.marker(Output.Verbosity.NORMAL)
                        .outstanding("* {} is up to date")
                        .say(ArtifactIdUtils.toId(artifact));
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
