/*
 * Copyright (c) 2023-2026 Maveniverse Org.
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
import java.util.stream.Stream;
import org.eclipse.aether.artifact.Artifact;

/**
 * Construction to supply collection of artifacts, for example like a filesystem directory.
 */
public final class DirectorySource implements Artifacts.Source {
    /**
     * Creates plain directory source, that supplies all artifacts it has.
     */
    public static DirectorySource directory(Path path) {
        return new DirectorySource(path);
    }

    private final Path directory;
    private final IndexFileReader indexFileReader;

    /**
     * Creates a directory source.
     *
     * @param directory The directory, if not existing, will be created.
     */
    private DirectorySource(Path directory) {
        this.directory = requireNonNull(directory, "directory").toAbsolutePath();
        if (!Files.isDirectory(directory)) {
            throw new IllegalArgumentException("directory must exists");
        }
        this.indexFileReader = new IndexFileReader(directory.resolve(".index"));
    }

    public Path getDirectory() {
        return directory;
    }

    @Override
    public Stream<Artifact> get() throws IOException {
        return indexFileReader.read(directory::resolve);
    }
}
