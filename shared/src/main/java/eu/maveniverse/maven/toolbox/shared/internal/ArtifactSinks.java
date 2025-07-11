/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static eu.maveniverse.maven.toolbox.shared.internal.ToolboxCommandoImpl.humanReadableByteCountBin;
import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.toolbox.shared.ArtifactMapper;
import eu.maveniverse.maven.toolbox.shared.ArtifactMatcher;
import eu.maveniverse.maven.toolbox.shared.ArtifactNameMapper;
import eu.maveniverse.maven.toolbox.shared.Sink;
import eu.maveniverse.maven.toolbox.shared.output.Output;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.internal.impl.checksum.Sha1ChecksumAlgorithmFactory;
import org.eclipse.aether.internal.impl.checksum.Sha512ChecksumAlgorithmFactory;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmHelper;

/**
 * Various utility sink implementations.
 */
public final class ArtifactSinks {
    private ArtifactSinks() {}

    public static Artifacts.Sink build(Map<String, ?> properties, ToolboxCommandoImpl tc, boolean dryRun, String spec) {
        requireNonNull(properties, "properties");
        requireNonNull(tc, "tc");
        requireNonNull(spec, "spec");
        ArtifactSinkBuilder builder = new ArtifactSinkBuilder(properties, tc, dryRun);
        SpecParser.parse(spec).accept(builder);
        return builder.build();
    }

    static class ArtifactSinkBuilder extends SpecParser.Builder {
        private final ToolboxCommandoImpl tc;
        private final boolean dryRun;

        public ArtifactSinkBuilder(Map<String, ?> properties, ToolboxCommandoImpl tc, boolean dryRun) {
            super(properties);
            this.tc = tc;
            this.dryRun = dryRun;
        }

        @Override
        public boolean visitEnter(SpecParser.Node node) {
            return super.visitEnter(node)
                    && !"flat".equals(node.getValue())
                    && !"matching".equals(node.getValue())
                    && !"mapping".equals(node.getValue())
                    && !"unpack".equals(node.getValue());
        }

        @Override
        protected void processOp(SpecParser.Node node) {
            switch (node.getValue()) {
                case "null": {
                    params.add(nullArtifactSink());
                    break;
                }
                case "counting": {
                    params.add(countingArtifactSink());
                    break;
                }
                case "sizing": {
                    params.add(sizingArtifactSink());
                    break;
                }
                case "logging": {
                    params.add(loggingArtifactSink(tc.output()));
                    break;
                }
                case "stat": {
                    params.add(statArtifactSink(0, booleanParam(node.getValue()), tc.output()));
                    break;
                }
                case "tee": {
                    params.add(teeArtifactSink(typedParams(Artifacts.Sink.class, node.getValue())));
                    break;
                }
                case "nonClosing": {
                    params.add(nonClosingArtifactSink(typedParam(Artifacts.Sink.class, node.getValue())));
                    break;
                }
                case "flat": {
                    try {
                        ArtifactNameMapper p1;
                        Path p0;
                        if (node.getChildren().size() == 2) {
                            ArtifactNameMapper.ArtifactNameMapperBuilder mapperBuilder =
                                    new ArtifactNameMapper.ArtifactNameMapperBuilder(properties);
                            node.getChildren().get(1).accept(mapperBuilder);
                            p1 = mapperBuilder.build();
                            p0 = tc.basedir().resolve(node.getChildren().get(0).getValue());
                        } else if (node.getChildren().size() == 1) {
                            p1 = ArtifactNameMapper.AbVCE();
                            p0 = tc.basedir().resolve(node.getChildren().get(0).getValue());
                        } else {
                            throw new IllegalArgumentException("op flat accepts only 1..2 argument");
                        }
                        params.add(DirectorySink.flat(tc.output(), p0, p1, dryRun));
                        node.getChildren().clear();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                    break;
                }
                case "repository": {
                    try {
                        Path p0 = tc.basedir().resolve(stringParam(node.getValue()));
                        params.add(DirectorySink.repository(tc.output(), p0, dryRun));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                    break;
                }
                case "install": {
                    if (node.getChildren().isEmpty()) {
                        params.add(InstallingSink.installing(tc.output(), tc.repositorySystem(), tc.session(), dryRun));
                    } else if (node.getChildren().size() == 1) {
                        String p0 = stringParam(node.getValue());
                        Path altLocalRepository = tc.basedir().resolve(p0);
                        LocalRepository localRepository = new LocalRepository(altLocalRepository.toFile());
                        LocalRepositoryManager lrm =
                                tc.repositorySystem().newLocalRepositoryManager(tc.session(), localRepository);
                        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession(tc.session());
                        session.setLocalRepositoryManager(lrm);
                        params.add(InstallingSink.installing(tc.output(), tc.repositorySystem(), session, dryRun));
                    } else {
                        throw new IllegalArgumentException("op install accepts only 0..1 argument");
                    }
                    break;
                }
                case "deploy": {
                    params.add(DeployingSink.deploying(
                            tc.output(),
                            tc.repositorySystem(),
                            tc.session(),
                            tc.parseRemoteRepository(stringParam(node.getValue())),
                            dryRun));
                    break;
                }
                case "purge": {
                    params.add(PurgingSink.purging(
                            tc.output(),
                            tc.repositorySystem(),
                            tc.session(),
                            stringParams(node.getValue()).stream()
                                    .map(tc::parseRemoteRepository)
                                    .collect(Collectors.toList()),
                            dryRun));
                    break;
                }
                case "unpack": {
                    try {
                        if (node.getChildren().size() == 1) {
                            Path p0 = tc.basedir()
                                    .resolve(node.getChildren().get(0).getValue());
                            params.add(UnpackSink.unpack(tc.output(), p0, ArtifactNameMapper.ACVE(), true, dryRun));
                        } else if (node.getChildren().size() == 2) {
                            ArtifactNameMapper.ArtifactNameMapperBuilder mapperBuilder =
                                    new ArtifactNameMapper.ArtifactNameMapperBuilder(properties);
                            node.getChildren().get(1).accept(mapperBuilder);
                            ArtifactNameMapper p1 = mapperBuilder.build();
                            Path p0 = tc.basedir()
                                    .resolve(node.getChildren().get(0).getValue());
                            params.add(UnpackSink.unpack(tc.output(), p0, p1, true, dryRun));
                        } else {
                            throw new IllegalArgumentException("op unpack accepts only 1..2 argument");
                        }
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                    node.getChildren().clear();
                    break;
                }
                case "matching": {
                    if (node.getChildren().size() != 2) {
                        throw new IllegalArgumentException("op matching accepts only 2 argument");
                    }
                    ArtifactMatcher.ArtifactMatcherBuilder matcherBuilder =
                            new ArtifactMatcher.ArtifactMatcherBuilder(properties);
                    node.getChildren().get(0).accept(matcherBuilder);
                    ArtifactMatcher matcher = matcherBuilder.build();
                    ArtifactSinkBuilder sinkBuilder = new ArtifactSinkBuilder(properties, tc, dryRun);
                    node.getChildren().get(1).accept(sinkBuilder);
                    Artifacts.Sink delegate = sinkBuilder.build();
                    params.add(matchingArtifactSink(matcher, delegate));
                    node.getChildren().clear();
                    break;
                }
                case "mapping": {
                    if (node.getChildren().size() != 2) {
                        throw new IllegalArgumentException("op mapping accepts only 2 argument");
                    }
                    ArtifactMapper.ArtifactMapperBuilder mapperBuilder =
                            new ArtifactMapper.ArtifactMapperBuilder(properties);
                    node.getChildren().get(0).accept(mapperBuilder);
                    ArtifactMapper mapper = mapperBuilder.build();
                    ArtifactSinkBuilder sinkBuilder = new ArtifactSinkBuilder(properties, tc, dryRun);
                    node.getChildren().get(1).accept(sinkBuilder);
                    Artifacts.Sink delegate = sinkBuilder.build();
                    params.add(mappingArtifactSink(mapper, delegate));
                    node.getChildren().clear();
                    break;
                }
                case "moduleDescriptor": {
                    params.add(new ModuleDescriptorExtractingSink(tc.output()));
                    break;
                }
                default:
                    throw new IllegalArgumentException("unknown op " + node.getValue());
            }
        }

        public Artifacts.Sink build() {
            return build(Artifacts.Sink.class);
        }
    }

    /**
     * Creates a "/dev/null" artifact sink.
     */
    public static NullArtifactSink nullArtifactSink() {
        return new NullArtifactSink();
    }

    public static class NullArtifactSink implements Artifacts.Sink {
        private NullArtifactSink() {}

        @Override
        public void accept(Collection<Artifact> artifacts) {}

        @Override
        public void accept(Artifact artifact) {}
    }

    public abstract static class DelegatingArtifactSink implements Artifacts.Sink {
        private final Sink<Artifact> delegate;

        public DelegatingArtifactSink(final Sink<Artifact> delegate) {
            this.delegate = requireNonNull(delegate, "delegate");
        }

        public void accept(Collection<Artifact> artifacts) throws IOException {
            delegate.accept(artifacts);
        }

        public void accept(final Artifact artifact) throws IOException {
            delegate.accept(artifact);
        }

        @Override
        public void cleanup(Exception e) {
            delegate.cleanup(e);
        }

        @Override
        public void close() throws Exception {
            delegate.close();
        }
    }

    /**
     * Creates a delegating sink that prevents closing delegate.
     */
    public static NonClosingArtifactSink nonClosingArtifactSink(Sink<Artifact> delegate) {
        requireNonNull(delegate, "delegate");
        return new NonClosingArtifactSink(delegate);
    }

    public static class NonClosingArtifactSink extends DelegatingArtifactSink {
        private NonClosingArtifactSink(Sink<Artifact> delegate) {
            super(delegate);
        }

        @Override
        public void close() {
            // do nothing
        }
    }

    /**
     * Creates a delegating sink that delegates calls only with matched artifacts.
     */
    public static MatchingArtifactSink matchingArtifactSink(
            Predicate<Artifact> artifactMatcher, Sink<Artifact> delegate) {
        requireNonNull(artifactMatcher, "artifactMatcher");
        requireNonNull(delegate, "delegate");
        return new MatchingArtifactSink(artifactMatcher, delegate);
    }

    public static class MatchingArtifactSink extends DelegatingArtifactSink {
        private final Predicate<Artifact> artifactMatcher;

        private MatchingArtifactSink(Predicate<Artifact> artifactMatcher, Sink<Artifact> delegate) {
            super(delegate);
            this.artifactMatcher = artifactMatcher;
        }

        @Override
        public void accept(Collection<Artifact> artifacts) throws IOException {
            super.accept(artifacts.stream().filter(artifactMatcher).collect(Collectors.toList()));
        }

        @Override
        public void accept(Artifact artifact) throws IOException {
            if (artifactMatcher.test(artifact)) {
                super.accept(artifact);
            }
        }
    }

    /**
     * Creates a delegating sink that delegates calls with mapped artifacts.
     */
    public static MappingArtifactSink mappingArtifactSink(
            Function<Artifact, Artifact> artifactMapper, Sink<Artifact> delegate) {
        requireNonNull(artifactMapper, "artifactMapper");
        requireNonNull(delegate, "delegate");
        return new MappingArtifactSink(artifactMapper, delegate);
    }

    public static class MappingArtifactSink extends DelegatingArtifactSink {
        private final Function<Artifact, Artifact> artifactMapper;

        private MappingArtifactSink(Function<Artifact, Artifact> artifactMapper, Sink<Artifact> delegate) {
            super(delegate);
            this.artifactMapper = artifactMapper;
        }

        @Override
        public void accept(Collection<Artifact> artifacts) throws IOException {
            super.accept(artifacts.stream().map(artifactMapper).collect(Collectors.toList()));
        }

        @Override
        public void accept(Artifact artifact) throws IOException {
            super.accept(artifactMapper.apply(artifact));
        }
    }

    /**
     * Creates a counting sink, that simply counts all the accepted artifacts.
     */
    public static CountingArtifactSink countingArtifactSink() {
        return new CountingArtifactSink();
    }

    public static class CountingArtifactSink implements Artifacts.Sink {
        private final LongAdder counter;

        private CountingArtifactSink() {
            this.counter = new LongAdder();
        }

        @Override
        public void accept(Artifact artifact) {
            counter.increment();
        }

        public int count() {
            return counter.intValue();
        }
    }

    /**
     * Creates a collecting sink, that simply collects all the accepted artifacts into a list.
     */
    public static CollectingArtifactSink collectingArtifactSink() {
        return new CollectingArtifactSink();
    }

    public static class CollectingArtifactSink implements Artifacts.Sink {
        private final CopyOnWriteArrayList<Artifact> artifacts;

        private CollectingArtifactSink() {
            this.artifacts = new CopyOnWriteArrayList<>();
        }

        @Override
        public void accept(Artifact artifact) {
            artifacts.add(artifact);
        }

        public List<Artifact> collect() {
            return artifacts;
        }
    }

    /**
     * Creates a sizing sink, that simply accumulate byte sizes of all accepted (and resolved) artifacts.
     */
    public static SizingArtifactSink sizingArtifactSink() {
        return new SizingArtifactSink();
    }

    public static class SizingArtifactSink implements Artifacts.Sink {
        private final LongAdder size;

        private SizingArtifactSink() {
            this.size = new LongAdder();
        }

        @Override
        public void accept(Artifact artifact) throws IOException {
            Path path = artifact.getFile() != null ? artifact.getFile().toPath() : null;
            if (path != null && Files.exists(path)) {
                size.add(Files.size(path));
            }
        }

        public long size() {
            return size.sum();
        }
    }

    /**
     * Creates a checksum sink, that simply accumulate checksums of seen (and resolved) artifacts.
     */
    public static ChecksumArtifactSink checksumArtifactSink(ChecksumAlgorithmFactory... factories) {
        return new ChecksumArtifactSink(
                factories.length == 0
                        ? List.of(new Sha1ChecksumAlgorithmFactory(), new Sha512ChecksumAlgorithmFactory())
                        : Arrays.asList(factories));
    }

    public static class ChecksumArtifactSink implements Artifacts.Sink {
        private final ConcurrentMap<Artifact, Map<String, String>> checksums = new ConcurrentHashMap<>();
        private final List<ChecksumAlgorithmFactory> algorithmFactories;

        private ChecksumArtifactSink(List<ChecksumAlgorithmFactory> algorithmFactories) {
            requireNonNull(algorithmFactories, "algorithmFactories");
            this.algorithmFactories = algorithmFactories;
        }

        @Override
        public void accept(Artifact artifact) throws IOException {
            Path path = artifact.getFile() != null ? artifact.getFile().toPath() : null;
            if (path != null && Files.exists(path)) {
                checksums.put(artifact, ChecksumAlgorithmHelper.calculate(path.toFile(), algorithmFactories));
            }
        }

        public Map<String, String> checksums(Artifact artifact) {
            return checksums.getOrDefault(artifact, Collections.emptyMap());
        }
    }

    /**
     * Creates a "tee" artifact sink out of supplied sinks.
     */
    @SafeVarargs
    public static TeeArtifactSink teeArtifactSink(Sink<Artifact>... artifactSinks) {
        return teeArtifactSink(Arrays.asList(artifactSinks));
    }

    /**
     * Creates a "tee" artifact sink out of supplied sinks.
     */
    public static TeeArtifactSink teeArtifactSink(Collection<? extends Sink<Artifact>> artifactSinks) {
        requireNonNull(artifactSinks, "artifactSinks");
        return new TeeArtifactSink(artifactSinks);
    }

    public static class TeeArtifactSink implements Artifacts.Sink {
        private final Collection<Sink<Artifact>> artifactSinks;

        private TeeArtifactSink(Collection<? extends Sink<Artifact>> artifactSinks) {
            this.artifactSinks = Collections.unmodifiableCollection(new ArrayList<>(artifactSinks));
        }

        @Override
        public void accept(Collection<Artifact> artifacts) throws IOException {
            for (Sink<Artifact> sink : artifactSinks) {
                sink.accept(artifacts);
            }
        }

        @Override
        public void accept(Artifact artifact) throws IOException {
            for (Sink<Artifact> sink : artifactSinks) {
                sink.accept(artifact);
            }
        }

        @Override
        public void cleanup(Exception e) {
            for (Sink<Artifact> sink : artifactSinks) {
                sink.cleanup(e);
            }
        }

        @Override
        public void close() throws Exception {
            for (Sink<Artifact> sink : artifactSinks) {
                sink.close();
            }
        }
    }

    /**
     * Creates a "stat" artifact sink out of supplied sinks.
     */
    public static StatArtifactSink statArtifactSink(int level, boolean details, Output output) {
        return new StatArtifactSink(level, details, output);
    }

    public static class StatArtifactSink implements Artifacts.Sink {
        private final int level;
        private final boolean details;
        private final Output output;
        private final CopyOnWriteArrayList<Artifact> seen = new CopyOnWriteArrayList<>();
        private final CountingArtifactSink countingArtifactSink = new CountingArtifactSink();
        private final SizingArtifactSink sizingArtifactSink = new SizingArtifactSink();
        private final ModuleDescriptorExtractingSink moduleDescriptorExtractingSink;
        private final ChecksumArtifactSink checksumArtifactSink;

        private StatArtifactSink(int level, boolean details, Output output) {
            this.level = level;
            this.details = details;
            this.output = requireNonNull(output, "output");
            this.moduleDescriptorExtractingSink = details ? new ModuleDescriptorExtractingSink(output) : null;
            this.checksumArtifactSink = details ? checksumArtifactSink() : null;
        }

        @Override
        public void accept(Artifact artifact) throws IOException {
            seen.add(artifact);
            countingArtifactSink.accept(artifact);
            sizingArtifactSink.accept(artifact);
            if (details) {
                moduleDescriptorExtractingSink.accept(artifact);
                checksumArtifactSink.accept(artifact);
            }
        }

        public List<Artifact> getSeenArtifacts() {
            return seen;
        }

        @Override
        public void close() throws Exception {
            String indent = "";
            for (int i = 0; i < level; i++) {
                indent += "  ";
            }
            countingArtifactSink.close();
            sizingArtifactSink.close();
            if (details) {
                moduleDescriptorExtractingSink.close();
                output.tell("{}------------------------------", indent);
                for (Artifact artifact : seen) {
                    ModuleDescriptorExtractingSink.ModuleDescriptor descriptor =
                            moduleDescriptorExtractingSink.getModuleDescriptor(artifact);
                    String moduleInfo = "";
                    if (descriptor != null) {
                        moduleInfo = moduleDescriptorExtractingSink.formatString(descriptor);
                    }
                    Map<String, String> checksums = checksumArtifactSink.checksums(artifact);
                    output.tell("{}{}", indent, artifact);
                    output.tell("{} -- {}", indent, moduleInfo);
                    if (checksums != null && !checksums.isEmpty()) {
                        for (Map.Entry<String, String> checksum : checksums.entrySet()) {
                            output.tell("{} -- {}: {}", indent, checksum.getKey(), checksum.getValue());
                        }
                    }
                }
                output.tell("{}------------------------------", indent);
            }
            output.doTell(
                    "{}Total of {} artifacts ({})",
                    indent,
                    countingArtifactSink.count(),
                    humanReadableByteCountBin(sizingArtifactSink.size()));
            output.tell("{}------------------------------", indent);
        }
    }

    /**
     * Creates a "logging" artifact sink that only logs the artifact it receives.
     */
    public static LoggingArtifactSink loggingArtifactSink(Output output) {
        return new LoggingArtifactSink(output);
    }

    public static class LoggingArtifactSink implements Artifacts.Sink {
        private final Output output;

        private LoggingArtifactSink(Output output) {
            this.output = requireNonNull(output, "output");
        }

        @Override
        public void accept(Artifact artifact) throws IOException {
            output.doTell(artifact.toString());
        }
    }
}
