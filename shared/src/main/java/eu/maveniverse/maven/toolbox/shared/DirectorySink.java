/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.toolbox.shared.internal.ArtifactMapper;
import eu.maveniverse.maven.toolbox.shared.internal.ArtifactMatcher;
import eu.maveniverse.maven.toolbox.shared.internal.ArtifactNameMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.Consumer;
import org.eclipse.aether.artifact.Artifact;

/**
 * Construction to accept collection of artifacts, for example like a filesystem directory.
 */
public final class DirectorySink implements Consumer<Collection<Artifact>> {

    public static DirectorySink flat(Output output, Path path) throws IOException {
        return new DirectorySink(output, path);
    }

    private final Output output;
    private final Path directory;
    private final ArtifactMatcher artifactMatcher;
    private final ArtifactMapper artifactMapper;
    private final ArtifactNameMapper artifactNameMapper;
    private final boolean allowOverwrite;
    private final HashSet<Path> writtenPaths;

    private DirectorySink(Output output, Path directory) throws IOException {
        this.output = requireNonNull(output, "output");
        this.directory = requireNonNull(directory, "directory");
        if (Files.exists(directory) && !Files.isDirectory(directory)) {
            throw new IllegalArgumentException("directory must not exists, or be a directory");
        }
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }

        this.artifactMatcher = ArtifactMatcher.any();
        this.artifactMapper = ArtifactMapper.identity();
        this.artifactNameMapper = ArtifactNameMapper.ACVE();
        this.allowOverwrite = false;
        this.writtenPaths = new HashSet<>();
    }

    @Override
    public void accept(Collection<Artifact> artifacts) {
        requireNonNull(artifacts, "artifacts");
        output.verbose("Copying {} artifacts to directory {}", artifacts.size(), directory);
        try {
            for (Artifact artifact : artifacts) {
                accept(artifact);
            }
        } catch (IOException e) {
            cleanup(artifacts, e);
        }
    }

    private void accept(Artifact artifact) throws IOException {
        output.verbose("Artifact {} processed", artifact);
        if (artifactMatcher.test(artifact)) {
            output.verbose("  matched");
            String name = artifactNameMapper.map(artifactMapper.map(artifact));
            output.verbose("  mapped to name {}", name);
            Path target = directory.resolve(name);
            if (!writtenPaths.add(target) && !allowOverwrite) {
                throw new IOException("Overwrite prevented: check mappings");
            }
            output.verbose("  copied to file {}", target);
            Files.copy(
                    artifact.getFile().toPath(),
                    target,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.COPY_ATTRIBUTES);
        }
    }

    private void cleanup(Collection<Artifact> artifacts, IOException e) {
        output.error("IO error happened, cleaning up", e);
        writtenPaths.forEach(p -> {
            try {
                Files.deleteIfExists(p);
            } catch (IOException ex) {
                // ignore
            }
        });
        throw new UncheckedIOException(e);
    }
}
