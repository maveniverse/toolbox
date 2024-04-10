/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.toolbox.shared.internal.SpecParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.eclipse.aether.artifact.Artifact;

/**
 * Various utility sink implementations.
 */
public final class ArtifactSinks {
    private ArtifactSinks() {}

    static ArtifactSink build(Map<String, ?> properties, Output output, String spec) {
        requireNonNull(properties, "properties");
        requireNonNull(output, "output");
        requireNonNull(spec, "spec");
        ArtifactSinkBuilder builder = new ArtifactSinkBuilder(properties, output);
        SpecParser.parse(spec).accept(builder);
        return builder.build();
    }

    static class ArtifactSinkBuilder extends SpecParser.Builder {
        private final Output output;

        public ArtifactSinkBuilder(Map<String, ?> properties, Output output) {
            super(properties);
            this.output = output;
        }

        @Override
        public boolean visitEnter(SpecParser.Node node) {
            return super.visitEnter(node) && !"matching".equals(node.getValue()) && !"mapping".equals(node.getValue());
        }

        @Override
        protected void processOp(SpecParser.Node node) {
            switch (node.getValue()) {
                case "null": {
                    params.add(nullArtifactSink());
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
                    ArtifactSinkBuilder sinkBuilder = new ArtifactSinkBuilder(properties, output);
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
                            new ArtifactMapper.ArtifactMapperBuilder(properties);
                    node.getChildren().get(0).accept(mapperBuilder);
                    ArtifactMapper mapper = mapperBuilder.build();
                    ArtifactSinkBuilder sinkBuilder = new ArtifactSinkBuilder(properties, output);
                    node.getChildren().get(1).accept(sinkBuilder);
                    ArtifactSink delegate = sinkBuilder.build();
                    params.add(mappingArtifactSink(mapper, delegate));
                    node.getChildren().clear();
                    break;
                }
                default:
                    throw new IllegalArgumentException("unknown op " + node.getValue());
            }
        }

        private ArtifactSink artifactSinkParam(String op) {
            if (params.isEmpty()) {
                throw new IllegalArgumentException("bad parameter count for " + op);
            }
            return (ArtifactSink) params.remove(params.size() - 1);
        }

        private ArtifactMapper artifactMapperParam(String op) {
            if (params.isEmpty()) {
                throw new IllegalArgumentException("bad parameter count for " + op);
            }
            return (ArtifactMapper) params.remove(params.size() - 1);
        }

        private ArtifactMatcher artifactMatcherParam(String op) {
            if (params.isEmpty()) {
                throw new IllegalArgumentException("bad parameter count for " + op);
            }
            return (ArtifactMatcher) params.remove(params.size() - 1);
        }

        private List<ArtifactMapper> artifactMapperParams(String op) {
            ArrayList<ArtifactMapper> result = new ArrayList<>();
            while (!params.isEmpty()) {
                if (params.get(params.size() - 1) instanceof ArtifactMapper) {
                    result.add(artifactMapperParam(op));
                } else {
                    break;
                }
            }
            return result;
        }

        public ArtifactSink build() {
            if (params.size() != 1) {
                throw new IllegalArgumentException("bad spec");
            }
            return (ArtifactSink) params.get(0);
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
    public static TeeArtifactSink teeArtifactSink(boolean doClose, ArtifactSink... artifactSinks) {
        return teeArtifactSink(doClose, Arrays.asList(artifactSinks));
    }

    /**
     * Creates a "tee" artifact sink out of supplied sinks.
     */
    public static TeeArtifactSink teeArtifactSink(boolean doClose, Collection<? extends ArtifactSink> artifactSinks) {
        requireNonNull(artifactSinks, "artifactSinks");
        return new TeeArtifactSink(doClose, artifactSinks);
    }

    public static class TeeArtifactSink implements ArtifactSink {
        private final boolean doClose;
        private final Collection<ArtifactSink> artifactSinks;

        private TeeArtifactSink(boolean doClose, Collection<? extends ArtifactSink> artifactSinks) {
            this.doClose = doClose;
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
            if (doClose) {
                for (ArtifactSink sink : artifactSinks) {
                    sink.close();
                }
            }
        }
    }
}
