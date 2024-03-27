/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.toolbox.shared.internal.ArtifactMatcher;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.LongAdder;
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

    /**
     * Creates a counting sink, that simply counts all the accepted artifacts.
     */
    public static CountingArtifactSink countingArtifactSink() {
        return countingArtifactSink(ArtifactMatcher.any());
    }

    /**
     * Creates a counting sink, that simply counts all the accepted and matched artifacts.
     */
    public static CountingArtifactSink countingArtifactSink(ArtifactMatcher artifactMatcher) {
        return new CountingArtifactSink(artifactMatcher);
    }

    public static class CountingArtifactSink implements ArtifactSink {
        private final LongAdder counter;
        private final ArtifactMatcher artifactMatcher;

        private CountingArtifactSink(ArtifactMatcher artifactMatcher) {
            this.counter = new LongAdder();
            this.artifactMatcher = requireNonNull(artifactMatcher, "artifactMatcher");
        }

        @Override
        public void accept(Artifact artifact) {
            if (artifactMatcher.test(artifact)) {
                counter.increment();
            }
        }

        public int count() {
            return counter.intValue();
        }
    }

    /**
     * Creates a sizing sink, that simply accumulate byte sizes of all accepted (and resolved) artifacts.
     */
    public static SizingArtifactSink sizingArtifactSink() {
        return sizingArtifactSink(ArtifactMatcher.any());
    }

    /**
     * Creates a sizing sink, that simply accumulate byte sizes of all accepted and matched (and resolved) artifacts.
     */
    public static SizingArtifactSink sizingArtifactSink(ArtifactMatcher artifactMatcher) {
        return new SizingArtifactSink(artifactMatcher);
    }

    public static class SizingArtifactSink implements ArtifactSink {
        private final LongAdder size;
        private final ArtifactMatcher artifactMatcher;

        private SizingArtifactSink(ArtifactMatcher artifactMatcher) {
            this.size = new LongAdder();
            this.artifactMatcher = requireNonNull(artifactMatcher, "artifactMatcher");
        }

        @Override
        public void accept(Artifact artifact) throws IOException {
            if (artifactMatcher.test(artifact)) {
                Path path = artifact.getFile() != null ? artifact.getFile().toPath() : null;
                if (path != null && Files.exists(path)) {
                    size.add(Files.size(path));
                }
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
        return teeArtifactSink(true, artifactSinks);
    }

    /**
     * Creates a "tee" artifact sink out of supplied sinks.
     */
    public static TeeArtifactSink teeArtifactSink(Collection<? extends ArtifactSink> artifactSinks) {
        return teeArtifactSink(true, artifactSinks);
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
