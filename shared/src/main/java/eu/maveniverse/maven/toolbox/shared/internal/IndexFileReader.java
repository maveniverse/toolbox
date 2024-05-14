/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;
import java.util.stream.Stream;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

/**
 * Reads "index file", that is a file having GAV per line.
 */
public final class IndexFileReader implements AutoCloseable {
    private final Path indexFile;

    public IndexFileReader(Path indexFile) {
        this.indexFile = requireNonNull(indexFile, "indexFile").toAbsolutePath();
    }

    public Stream<Artifact> read(Function<String, Path> pathResolver) throws IOException {
        return Files.readAllLines(indexFile).stream()
                .filter(l -> !l.trim().isEmpty() && !l.startsWith("#"))
                .map(l -> {
                    String[] parts = l.split(" >> ", -1);
                    return new DefaultArtifact(parts[0])
                            .setFile(pathResolver.apply(parts[1]).toFile());
                });
    }

    @Override
    public void close() {}
}
