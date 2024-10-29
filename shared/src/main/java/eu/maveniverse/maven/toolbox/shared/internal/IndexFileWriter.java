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
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;

/**
 * Writes "index file", that is a file having GAV per line.
 */
public final class IndexFileWriter implements AutoCloseable {
    private final Path indexFile;
    private final Path file;
    private final boolean dryRun;
    private final AtomicBoolean failed;
    private final AtomicBoolean closed;
    private final PrintWriter printWriter;

    public IndexFileWriter(Path indexFile, boolean append, boolean dryRun) throws IOException {
        this.indexFile = requireNonNull(indexFile, "indexFile").toAbsolutePath();
        this.file = indexFile
                .getParent()
                .resolve(".index-" + ThreadLocalRandom.current().nextInt());
        if (Files.isRegularFile(this.indexFile) && append) {
            Files.copy(this.indexFile, this.file);
        }
        this.dryRun = dryRun;
        this.failed = new AtomicBoolean(false);
        this.closed = new AtomicBoolean(false);

        this.printWriter = new PrintWriter(Files.newBufferedWriter(
                file,
                append
                        ? new StandardOpenOption[] {
                            StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND
                        }
                        : new StandardOpenOption[] {
                            StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING
                        }));
    }

    public void write(Artifact artifact, String path) {
        requireNonNull(artifact, "artifact");
        requireNonNull(path, "path");
        if (closed.get()) {
            throw new IllegalStateException("already closed");
        }
        if (!failed.get() && !dryRun) {
            printWriter.println(ArtifactIdUtils.toId(artifact) + " >> " + path);
        }
    }

    public void fail() {
        failed.set(true);
    }

    @Override
    public void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
            printWriter.flush();
            printWriter.close();
            if (failed.get()) {
                Files.deleteIfExists(file);
            } else {
                Files.move(file, indexFile, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
}
