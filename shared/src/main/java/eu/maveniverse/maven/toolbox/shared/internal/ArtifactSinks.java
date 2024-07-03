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
import eu.maveniverse.maven.toolbox.shared.ArtifactSink;
import eu.maveniverse.maven.toolbox.shared.Output;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.version.VersionScheme;

/**
 * Various utility sink implementations.
 */
public final class ArtifactSinks {
    private ArtifactSinks() {}

    public static ArtifactSink build(
            VersionScheme versionScheme, Map<String, ?> properties, ToolboxCommandoImpl tc, String spec) {
        requireNonNull(properties, "properties");
        requireNonNull(tc, "tc");
        requireNonNull(spec, "spec");
        ArtifactSinkBuilder builder = new ArtifactSinkBuilder(versionScheme, properties, tc);
        SpecParser.parse(spec).accept(builder);
        return builder.build();
    }

    static class ArtifactSinkBuilder extends SpecParser.Builder {
        private final ToolboxCommandoImpl tc;

        public ArtifactSinkBuilder(VersionScheme versionScheme, Map<String, ?> properties, ToolboxCommandoImpl tc) {
            super(versionScheme, properties);
            this.tc = tc;
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
                case "tee": {
                    params.add(teeArtifactSink(typedParams(ArtifactSink.class, node.getValue())));
                    break;
                }
                case "nonClosing": {
                    params.add(nonClosingArtifactSink(typedParam(ArtifactSink.class, node.getValue())));
                    break;
                }
                case "flat": {
                    try {
                        ArtifactNameMapper p1;
                        Path p0;
                        if (node.getChildren().size() == 2) {
                            ArtifactNameMapper.ArtifactNameMapperBuilder mapperBuilder =
                                    new ArtifactNameMapper.ArtifactNameMapperBuilder(versionScheme, properties);
                            node.getChildren().get(1).accept(mapperBuilder);
                            p1 = mapperBuilder.build();
                            p0 = tc.getContext()
                                    .basedir()
                                    .resolve(node.getChildren().get(0).getValue());
                        } else if (node.getChildren().size() == 1) {
                            p1 = ArtifactNameMapper.AbVCE();
                            p0 = tc.getContext()
                                    .basedir()
                                    .resolve(node.getChildren().get(0).getValue());
                        } else {
                            throw new IllegalArgumentException("op flat accepts only 1..2 argument");
                        }
                        params.add(DirectorySink.flat(p0, p1));
                        node.getChildren().clear();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                    break;
                }
                case "repository": {
                    try {
                        Path p0 = tc.getContext().basedir().resolve(stringParam(node.getValue()));
                        params.add(DirectorySink.repository(p0));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                    break;
                }
                case "install": {
                    if (node.getChildren().isEmpty()) {
                        params.add(InstallingSink.installing(
                                tc.getToolboxResolver().getRepositorySystem(),
                                tc.getToolboxResolver().getSession()));
                    } else if (node.getChildren().size() == 1) {
                        String p0 = stringParam(node.getValue());
                        Path altLocalRepository = tc.getContext().basedir().resolve(p0);
                        LocalRepository localRepository = new LocalRepository(altLocalRepository.toFile());
                        LocalRepositoryManager lrm = tc.getToolboxResolver()
                                .getRepositorySystem()
                                .newLocalRepositoryManager(
                                        tc.getToolboxResolver().getSession(), localRepository);
                        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession(
                                tc.getToolboxResolver().getSession());
                        session.setLocalRepositoryManager(lrm);
                        params.add(InstallingSink.installing(
                                tc.getToolboxResolver().getRepositorySystem(), session));
                    } else {
                        throw new IllegalArgumentException("op install accepts only 0..1 argument");
                    }
                    break;
                }
                case "deploy": {
                    params.add(DeployingSink.deploying(
                            tc.getToolboxResolver().getRepositorySystem(),
                            tc.getToolboxResolver().getSession(),
                            tc.parseRemoteRepository(stringParam(node.getValue()))));
                    break;
                }
                case "purge": {
                    params.add(PurgingSink.purging(
                            tc.getToolboxResolver().getRepositorySystem(),
                            tc.getToolboxResolver().getSession(),
                            stringParams(node.getValue()).stream()
                                    .map(tc.getToolboxResolver()::parseRemoteRepository)
                                    .collect(Collectors.toList())));
                    break;
                }
                case "unpack": {
                    try {
                        if (node.getChildren().size() == 1) {
                            Path p0 = tc.getContext()
                                    .basedir()
                                    .resolve(node.getChildren().get(0).getValue());
                            params.add(UnpackSink.unpack(p0, ArtifactNameMapper.ACVE(), true));
                        } else if (node.getChildren().size() == 2) {
                            ArtifactNameMapper.ArtifactNameMapperBuilder mapperBuilder =
                                    new ArtifactNameMapper.ArtifactNameMapperBuilder(versionScheme, properties);
                            node.getChildren().get(1).accept(mapperBuilder);
                            ArtifactNameMapper p1 = mapperBuilder.build();
                            Path p0 = tc.getContext()
                                    .basedir()
                                    .resolve(node.getChildren().get(0).getValue());
                            params.add(UnpackSink.unpack(p0, p1, true));
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
                            new ArtifactMatcher.ArtifactMatcherBuilder(versionScheme, properties);
                    node.getChildren().get(0).accept(matcherBuilder);
                    ArtifactMatcher matcher = matcherBuilder.build();
                    ArtifactSinkBuilder sinkBuilder = new ArtifactSinkBuilder(versionScheme, properties, tc);
                    node.getChildren().get(1).accept(sinkBuilder);
                    ArtifactSink delegate = sinkBuilder.build();
                    params.add(matchingArtifactSink(matcher, delegate));
                    node.getChildren().clear();
                    break;
                }
                case "mapping": {
                    if (node.getChildren().size() != 2) {
                        throw new IllegalArgumentException("op mapping accepts only 2 argument");
                    }
                    ArtifactMapper.ArtifactMapperBuilder mapperBuilder =
                            new ArtifactMapper.ArtifactMapperBuilder(versionScheme, properties);
                    node.getChildren().get(0).accept(mapperBuilder);
                    ArtifactMapper mapper = mapperBuilder.build();
                    ArtifactSinkBuilder sinkBuilder = new ArtifactSinkBuilder(versionScheme, properties, tc);
                    node.getChildren().get(1).accept(sinkBuilder);
                    ArtifactSink delegate = sinkBuilder.build();
                    params.add(mappingArtifactSink(mapper, delegate));
                    node.getChildren().clear();
                    break;
                }
                case "moduleDescriptor": {
                    params.add(new ModuleDescriptorExtractingSink());
                    break;
                }
                default:
                    throw new IllegalArgumentException("unknown op " + node.getValue());
            }
        }

        public ArtifactSink build() {
            return build(ArtifactSink.class);
        }
    }

    /**
     * Creates a "/dev/null" artifact sink.
     */
    public static NullArtifactSink nullArtifactSink() {
        return new NullArtifactSink();
    }

    public static class NullArtifactSink implements ArtifactSink {
        private NullArtifactSink() {}

        @Override
        public void accept(Collection<Artifact> artifacts) {}

        @Override
        public void accept(Artifact artifact) {}
    }

    public abstract static class DelegatingArtifactSink implements ArtifactSink {
        private final ArtifactSink delegate;

        public DelegatingArtifactSink(final ArtifactSink delegate) {
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
    public static NonClosingArtifactSink nonClosingArtifactSink(ArtifactSink delegate) {
        requireNonNull(delegate, "delegate");
        return new NonClosingArtifactSink(delegate);
    }

    public static class NonClosingArtifactSink extends DelegatingArtifactSink {
        private NonClosingArtifactSink(ArtifactSink delegate) {
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
            Predicate<Artifact> artifactMatcher, ArtifactSink delegate) {
        requireNonNull(artifactMatcher, "artifactMatcher");
        requireNonNull(delegate, "delegate");
        return new MatchingArtifactSink(artifactMatcher, delegate);
    }

    public static class MatchingArtifactSink extends DelegatingArtifactSink {
        private final Predicate<Artifact> artifactMatcher;

        private MatchingArtifactSink(Predicate<Artifact> artifactMatcher, ArtifactSink delegate) {
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
            Function<Artifact, Artifact> artifactMapper, ArtifactSink delegate) {
        requireNonNull(artifactMapper, "artifactMapper");
        requireNonNull(delegate, "delegate");
        return new MappingArtifactSink(artifactMapper, delegate);
    }

    public static class MappingArtifactSink extends DelegatingArtifactSink {
        private final Function<Artifact, Artifact> artifactMapper;

        private MappingArtifactSink(Function<Artifact, Artifact> artifactMapper, ArtifactSink delegate) {
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

    public static class CountingArtifactSink implements ArtifactSink {
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
     * Creates a sizing sink, that simply accumulate byte sizes of all accepted (and resolved) artifacts.
     */
    public static SizingArtifactSink sizingArtifactSink() {
        return new SizingArtifactSink();
    }

    public static class SizingArtifactSink implements ArtifactSink {
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
     * Creates a "tee" artifact sink out of supplied sinks.
     */
    public static TeeArtifactSink teeArtifactSink(ArtifactSink... artifactSinks) {
        return teeArtifactSink(Arrays.asList(artifactSinks));
    }

    /**
     * Creates a "tee" artifact sink out of supplied sinks.
     */
    public static TeeArtifactSink teeArtifactSink(Collection<? extends ArtifactSink> artifactSinks) {
        requireNonNull(artifactSinks, "artifactSinks");
        return new TeeArtifactSink(artifactSinks);
    }

    public static class TeeArtifactSink implements ArtifactSink {
        private final Collection<ArtifactSink> artifactSinks;

        private TeeArtifactSink(Collection<? extends ArtifactSink> artifactSinks) {
            this.artifactSinks = Collections.unmodifiableCollection(new ArrayList<>(artifactSinks));
        }

        @Override
        public void accept(Collection<Artifact> artifacts) throws IOException {
            for (ArtifactSink sink : artifactSinks) {
                sink.accept(artifacts);
            }
        }

        @Override
        public void accept(Artifact artifact) throws IOException {
            for (ArtifactSink sink : artifactSinks) {
                sink.accept(artifact);
            }
        }

        @Override
        public void cleanup(Exception e) {
            for (ArtifactSink sink : artifactSinks) {
                sink.cleanup(e);
            }
        }

        @Override
        public void close() throws Exception {
            for (ArtifactSink sink : artifactSinks) {
                sink.close();
            }
        }
    }

    /**
     * Creates a "stat" artifact sink out of supplied sinks.
     */
    public static StatArtifactSink statArtifactSink(int level, boolean moduleDescriptor, Output output) {
        return new StatArtifactSink(level, moduleDescriptor, output);
    }

    public static class StatArtifactSink implements ArtifactSink {
        private final int level;
        private final Output output;
        private final CountingArtifactSink countingArtifactSink = new CountingArtifactSink();
        private final SizingArtifactSink sizingArtifactSink = new SizingArtifactSink();
        private final ModuleDescriptorExtractingSink moduleDescriptorExtractingSink;

        private StatArtifactSink(int level, boolean moduleDescriptor, Output output) {
            this.level = level;
            this.output = requireNonNull(output, "output");
            this.moduleDescriptorExtractingSink = moduleDescriptor ? new ModuleDescriptorExtractingSink() : null;
        }

        @Override
        public void accept(Artifact artifact) throws IOException {
            countingArtifactSink.accept(artifact);
            sizingArtifactSink.accept(artifact);
            if (moduleDescriptorExtractingSink != null) {
                moduleDescriptorExtractingSink.accept(artifact);
            }
        }

        @Override
        public void close() throws Exception {
            String indent = "";
            for (int i = 0; i < level; i++) {
                indent += "  ";
            }
            countingArtifactSink.close();
            sizingArtifactSink.close();
            if (moduleDescriptorExtractingSink != null) {
                moduleDescriptorExtractingSink.close();
                output.normal("{}------------------------------", indent);
                for (Map.Entry<Artifact, ModuleDescriptorExtractingSink.ModuleDescriptor> entry :
                        moduleDescriptorExtractingSink.getModuleDescriptors().entrySet()) {
                    String moduleInfo = "";
                    if (entry.getValue() != null) {
                        ModuleDescriptorExtractingSink.ModuleDescriptor moduleDescriptor = entry.getValue();
                        moduleInfo = moduleDescriptorExtractingSink.formatString(moduleDescriptor);
                    }
                    if (output.isVerbose()) {
                        output.verbose(
                                "{}{} {} -> {}",
                                indent,
                                entry.getKey(),
                                moduleInfo,
                                entry.getKey().getFile());
                    } else {
                        output.normal("{}{} {}", indent, entry.getKey(), moduleInfo);
                    }
                }
                output.normal("{}------------------------------", indent);
            }
            output.normal(
                    "{}Total of {} artifacts ({})",
                    indent,
                    countingArtifactSink.count(),
                    humanReadableByteCountBin(sizingArtifactSink.size()));
            output.normal("{}------------------------------", indent);
        }
    }
}
