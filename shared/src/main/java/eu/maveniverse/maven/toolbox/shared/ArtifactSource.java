/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.aether.artifact.Artifact;

/**
 * Construction to supply collection of artifacts, for example like a filesystem directory.
 */
public interface ArtifactSource extends AutoCloseable {
    Stream<Artifact> get() throws IOException;

    default List<Artifact> getAllArtifacts() throws IOException {
        try (Stream<Artifact> stream = get()) {
            return stream.collect(Collectors.toList());
        }
    }

    @Override
    default void close() throws IOException {}
}
