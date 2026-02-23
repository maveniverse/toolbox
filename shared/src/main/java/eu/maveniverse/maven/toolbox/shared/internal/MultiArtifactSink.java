/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.toolbox.shared.Sink;
import eu.maveniverse.maven.toolbox.shared.output.Output;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;
import org.eclipse.aether.artifact.Artifact;

/**
 * Construction to accept collection of artifacts and differentiate the into separate "lanes", or sinks.
 */
public final class MultiArtifactSink implements Artifacts.Sink {
    public static final class MultiArtifactSinkBuilder {
        private final Output output;
        private final LinkedHashMap<Predicate<Artifact>, Sink<Artifact>> sinks;
        private boolean missedArtifactFails = true;

        private MultiArtifactSinkBuilder(Output output) {
            this.output = requireNonNull(output, "output");
            this.sinks = new LinkedHashMap<>();
        }

        public MultiArtifactSinkBuilder setMissedArtifactFails(boolean missedArtifactFails) {
            this.missedArtifactFails = missedArtifactFails;
            return this;
        }

        public MultiArtifactSinkBuilder addSink(Predicate<Artifact> artifactMatcher, Sink<Artifact> sink) {
            requireNonNull(artifactMatcher, "artifactMatcher");
            requireNonNull(sink, "sink");
            sinks.put(artifactMatcher, sink);
            return this;
        }

        public MultiArtifactSink build() {
            return new MultiArtifactSink(output, sinks, missedArtifactFails);
        }
    }

    /**
     * Creates new empty instance.
     */
    public static MultiArtifactSinkBuilder builder(Output output) {
        return new MultiArtifactSinkBuilder(output);
    }

    private final Output output;
    private final Map<Predicate<Artifact>, Sink<Artifact>> sinks;
    private final boolean missedArtifactFails;

    private MultiArtifactSink(
            Output output, LinkedHashMap<Predicate<Artifact>, Sink<Artifact>> sinks, boolean missedArtifactFails) {
        this.output = requireNonNull(output, "output");
        this.sinks = Collections.unmodifiableMap(sinks);
        this.missedArtifactFails = missedArtifactFails;
    }

    @Override
    public void accept(Artifact artifact) throws IOException {
        boolean processed = false;
        for (Map.Entry<Predicate<Artifact>, Sink<Artifact>> sink : sinks.entrySet()) {
            if (sink.getKey().test(artifact)) {
                sink.getValue().accept(artifact);
                processed = true;
                break;
            }
        }
        if (!processed && missedArtifactFails) {
            throw new IllegalStateException("Nobody accepted artifact: " + artifact);
        }
        output.chatter("Processed {}: {}", artifact, processed ? "OK" : "MISSED");
    }

    @Override
    public void cleanup(Exception e) {
        sinks.values().forEach(a -> a.cleanup(e));
    }

    @Override
    public void close() throws Exception {
        ArrayList<Exception> exceptions = new ArrayList<>();
        for (Sink<Artifact> sink : sinks.values()) {
            try {
                sink.close();
            } catch (Exception e) {
                exceptions.add(e);
            }
        }
        if (!exceptions.isEmpty()) {
            IllegalStateException ex = new IllegalStateException("Closing failed");
            exceptions.forEach(ex::addSuppressed);
            throw ex;
        }
    }
}
