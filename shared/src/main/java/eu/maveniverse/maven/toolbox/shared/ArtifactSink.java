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
import org.eclipse.aether.artifact.Artifact;

/**
 * Construction to accept collection of artifacts, for example like a filesystem directory.
 */
public interface ArtifactSink extends AutoCloseable {
    default void accept(Collection<Artifact> artifacts) throws IOException {
        requireNonNull(artifacts, "artifacts");
        try {
            for (Artifact artifact : artifacts) {
                accept(artifact);
            }
        } catch (IOException e) {
            cleanup(e);
            throw e;
        }
    }

    void accept(Artifact artifact) throws IOException;

    default void cleanup(IOException e) {}

    default void close() throws Exception {}
}
