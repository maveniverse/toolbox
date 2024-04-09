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
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.eclipse.aether.artifact.Artifact;

/**
 * Construction to accept collection of artifacts and apply mapping and pass to delegate sink.
 */
public final class MappingArtifactSink implements ArtifactSink {
    /**
     * Creates mapping sink that maps and delegates to passed in delegate.
     */
    public static MappingArtifactSink mapping(Output output, ArtifactMapper artifactMapper, ArtifactSink delegate) {
        return new MappingArtifactSink(output, artifactMapper, delegate);
    }

    private final Output output;
    private final Function<Artifact, Artifact> artifactMapper;
    private final ArtifactSink delegate;

    private MappingArtifactSink(Output output, Function<Artifact, Artifact> artifactMapper, ArtifactSink delegate) {
        this.output = requireNonNull(output, "output");
        this.artifactMapper = requireNonNull(artifactMapper, "artifactMapper");
        this.delegate = requireNonNull(delegate, "delegate");
    }

    @Override
    public void accept(Collection<Artifact> artifacts) throws IOException {
        output.verbose("Accept artifacts {}", artifacts);
        delegate.accept(artifacts.stream().map(artifactMapper).collect(Collectors.toList()));
    }

    @Override
    public void accept(Artifact artifact) throws IOException {
        output.verbose("Accept artifact {}", artifact);
        delegate.accept(artifactMapper.apply(artifact));
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
