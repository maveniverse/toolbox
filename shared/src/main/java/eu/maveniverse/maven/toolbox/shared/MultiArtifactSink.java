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
import java.util.LinkedHashMap;
import java.util.Map;
import org.eclipse.aether.artifact.Artifact;

/**
 * Construction to accept collection of artifacts and differentiate the into separate "lanes", or sinks.
 */
public final class MultiArtifactSink implements ArtifactSink {
    /**
     * Creates new empty instance.
     */
    public static MultiArtifactSink multi(Output output) {
        return new MultiArtifactSink(output);
    }

    private final Output output;
    private final LinkedHashMap<ArtifactMatcher, ArtifactSink> sinks;

    /**
     * Creates a multi artifact sink.
     *
     * @param output The output.
     */
    private MultiArtifactSink(Output output) {
        this.output = requireNonNull(output, "output");
        this.sinks = new LinkedHashMap<>();
    }

    public MultiArtifactSink addSink(ArtifactMatcher artifactMatcher, ArtifactSink sink) {
        requireNonNull(artifactMatcher, "artifactMatcher");
        requireNonNull(sink, "sink");
        sinks.put(artifactMatcher, sink);
        return this;
    }

    @Override
    public void accept(Artifact artifact) throws IOException {
        output.verbose("Accept artifact {}", artifact);
        boolean processed = false;
        for (Map.Entry<ArtifactMatcher, ArtifactSink> sink : sinks.entrySet()) {
            if (sink.getKey().test(artifact)) {
                sink.getValue().accept(artifact);
                processed = true;
                break;
            }
        }
        if (!processed) {
            output.verbose("Nobody accepted artifact {}", artifact);
        }
    }

    @Override
    public void cleanup(Exception e) {
        sinks.values().forEach(a -> a.cleanup(e));
    }

    @Override
    public void close() {
        for (ArtifactSink sink : sinks.values()) {
            try {
                sink.close();
            } catch (Exception e) {
                output.warn("Closing sink failed", e);
            }
        }
    }
}
