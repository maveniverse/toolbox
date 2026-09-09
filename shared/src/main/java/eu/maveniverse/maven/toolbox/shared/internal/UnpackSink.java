/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static java.util.Objects.requireNonNull;

import ca.vanzyl.provisio.archive.UnArchiver;
import eu.maveniverse.maven.toolbox.shared.ArtifactMatcher;
import eu.maveniverse.maven.toolbox.shared.output.Output;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.eclipse.aether.artifact.Artifact;

/**
 * Construction to accept collection of artifacts and unpack them.
 */
public final class UnpackSink implements Artifacts.Sink {
    /**
     * Creates plain unpack sink where unpacking happens according to supplied parameters.
     *
     * @param path The root where unpack happens.
     * @param artifactRootMapper The artifact root mapper, that decides where is root of unpacking for given artifact.
     *                           To achieve "overlay", one can use {@code fixed(.)} mapper that will map roots into
     *                           root of {@code path} parameter.
     */
    public static UnpackSink unpack(
            Output output, Path path, Function<Artifact, String> artifactRootMapper, boolean dryRun)
            throws IOException {
        return new UnpackSink(output, path, ArtifactMatcher.unique(), false, a -> a, artifactRootMapper, true, dryRun);
    }

    private final Output output;
    private final Path directory;
    private final boolean directoryCreated;
    private final Predicate<Artifact> artifactMatcher;
    private final boolean failIfUnmatched;
    private final Function<Artifact, Artifact> artifactMapper;
    private final Function<Artifact, String> artifactRootMapper;
    private final boolean allowRootOverwrite;
    private final boolean dryRun;
    private final HashSet<Path> writtenPaths;

    /**
     * Creates a directory sink.
     *
     * @param output The output.
     * @param directory The directory, if not existing, will be created.
     * @param artifactMatcher The matcher, that decides is this sink accepting artifact or not.
     * @param artifactMapper The artifact mapper, that may re-map artifact.
     * @param artifactRootMapper The artifact root mapper, that decides where is root of unpacking for given artifact.
     * @param allowRootOverwrite Does sink allow use of same roots for unpack operations.
     * @throws IOException In case of IO problem.
     */
    private UnpackSink(
            Output output,
            Path directory,
            Predicate<Artifact> artifactMatcher,
            boolean failIfUnmatched,
            Function<Artifact, Artifact> artifactMapper,
            Function<Artifact, String> artifactRootMapper,
            boolean allowRootOverwrite,
            boolean dryRun)
            throws IOException {
        this.output = requireNonNull(output, "output");
        this.directory = requireNonNull(directory, "directory").toAbsolutePath();
        if (Files.exists(directory) && !Files.isDirectory(directory)) {
            throw new IllegalArgumentException("directory must not exists, or must be a directory");
        }
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
            this.directoryCreated = true;
        } else {
            this.directoryCreated = false;
        }

        this.artifactMatcher = requireNonNull(artifactMatcher, "artifactMatcher");
        this.failIfUnmatched = failIfUnmatched;
        this.artifactMapper = requireNonNull(artifactMapper, "artifactMapper");
        this.artifactRootMapper = requireNonNull(artifactRootMapper, "artifactRootMapper");
        this.allowRootOverwrite = allowRootOverwrite;
        this.dryRun = dryRun;
        this.writtenPaths = new HashSet<>();
    }

    public Path getDirectory() {
        return directory;
    }

    @Override
    public void accept(Artifact artifact) throws IOException {
        requireNonNull(artifact, "artifact");
        output.chatter("Accept artifact {}", artifact);
        if (artifactMatcher.test(artifact)) {
            output.chatter("  matched");
            String targetName = artifactRootMapper.apply(artifactMapper.apply(artifact));
            output.chatter("  mapped to name {}", targetName);
            Path target = directory.resolve(targetName).toAbsolutePath();
            if (!target.startsWith(directory)) {
                throw new IOException("Path escape prevented; check mappings");
            }
            if (!writtenPaths.add(target) && !allowRootOverwrite) {
                throw new IOException("Root overwrite prevented; check mappings");
            }
            unpack(artifact.getFile().toPath(), target, true);
        } else {
            if (failIfUnmatched) {
                throw new IllegalArgumentException("not matched");
            }
        }
    }

    /**
     * Unpacks file to given directory. Supports ZIP and TAR.
     */
    private void unpack(Path source, Path target, boolean useRoot) throws IOException {
        requireNonNull(source);
        requireNonNull(target);
        if (!Files.isRegularFile(source)) {
            throw new IllegalArgumentException("source is not a regular file");
        }
        Files.createDirectories(target);
        UnArchiver.builder().useRoot(useRoot).build().unarchive(source, target);
    }

    @Override
    public void cleanup(Exception e) {
        if (dryRun) {
            return;
        }
        writtenPaths.forEach(p -> {
            try (Stream<Path> stream = Files.walk(p).sorted(Comparator.reverseOrder())) {
                stream.forEach(f -> {
                    try {
                        Files.delete(f);
                    } catch (IOException ioex) {
                        output.warn("Could not delete {}", p, ioex);
                    }
                });
            } catch (IOException ioex) {
                output.warn("Could not walk {}", p, ioex);
            }
        });
        if (directoryCreated) {
            try {
                Files.deleteIfExists(directory);
            } catch (IOException ex) {
                // ignore
            }
        }
    }

    @Override
    public void close() {}
}
