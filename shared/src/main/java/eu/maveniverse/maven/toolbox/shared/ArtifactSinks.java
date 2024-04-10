/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
