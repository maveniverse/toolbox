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
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;

/**
 * Construction to accept collection of artifacts into "index file", that is a file having GAV per line.
 */
public final class IndexFileSink implements ArtifactSink {
    /**
     * Creates plain "flat" index file sink.
     */
    public static IndexFileSink flat(Output output, Path path, boolean append) throws IOException {
        return new IndexFileSink(output, path, append);
    }

    private final Output output;
    private final Path file;
    private final Path backupFile;
    private final AtomicBoolean closed;
    private final PrintWriter printWriter;

    /**
     * Creates a directory sink.
     *
     * @param output The output.
     * @param file The file, if not existing, will be created.
     * @param append Should be the file appended (if exists) of overwritten.
     * @throws IOException In case of IO problem.
     */
    private IndexFileSink(Output output, Path file, boolean append) throws IOException {
        this.output = requireNonNull(output, "output");
        this.file = requireNonNull(file, "file").toAbsolutePath();
        if (Files.isRegularFile(this.file)) {
            this.backupFile = file.getParent().resolve(file.getFileName() + ".bak");
            Files.copy(this.file, this.backupFile);
        } else {
            this.backupFile = null;
        }
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

    public Path getFile() {
        return file;
    }

    @Override
    public void accept(Artifact artifact) throws IOException {
        requireNonNull(artifact, "artifact");
        if (closed.get()) {
            throw new IllegalStateException("already closed");
        }
        printWriter.println(ArtifactIdUtils.toId(artifact));
    }

    @Override
    public void cleanup(Exception e) {
        output.error("Cleaning up: {}", file);
        try {
            close();
            if (backupFile != null) {
                Files.move(backupFile, file, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.deleteIfExists(file);
            }
        } catch (IOException ex) {
            // ignore
        }
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            printWriter.flush();
            printWriter.close();
        }
    }
}
