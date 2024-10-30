/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.toolbox.shared.ArtifactMatcher;
import eu.maveniverse.maven.toolbox.shared.ArtifactNameMapper;
import eu.maveniverse.maven.toolbox.shared.output.Output;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.function.Function;
import java.util.function.Predicate;
import org.eclipse.aether.artifact.Artifact;

/**
 * Construction to accept collection of artifacts, for example like a filesystem directory.
 */
public final class DirectorySink implements Artifacts.Sink {
    /**
     * Creates plain "flat" directory sink, that accepts all artifacts and copies them out having filenames according
     * to supplied {@link ArtifactNameMapper} and prevents overwrite (what you usually want).
     */
    public static DirectorySink flat(Output output, Path path, ArtifactNameMapper artifactNameMapper, boolean dryRun)
            throws IOException {
        return new DirectorySink(
                output, path, Mode.COPY, ArtifactMatcher.unique(), false, artifactNameMapper, false, dryRun);
    }

    /**
     * Creates "repository" directory sink, that accepts all non-snapshot artifacts and copies them out having
     * filenames as a "remote repository" (usable as file based remote repository, or can be published via HTTP). It
     * also prevents overwrite (what you usually want). This repository may be handy for testing, but does not serve
     * as interchangeable solution of installing or deploying artifacts for real. This sink accepts release artifacts
     * only, and fails with snapshot ones, as this is not equivalent to deploy them (no timestamped version is
     * created).
     */
    public static DirectorySink repository(Output output, Path path, boolean dryRun) throws IOException {
        return new DirectorySink(
                output,
                path,
                Mode.COPY,
                ArtifactMatcher.and(ArtifactMatcher.not(ArtifactMatcher.snapshot()), ArtifactMatcher.unique()),
                true,
                ArtifactNameMapper.repositoryDefault(),
                false,
                dryRun);
    }

    /**
     * Writing mode.
     */
    public enum Mode {
        COPY,
        LINK,
        SYMLINK
    }

    private final Output output;
    private final Path directory;
    private final boolean directoryCreated;
    private final Mode mode;
    private final Predicate<Artifact> artifactMatcher;
    private final boolean failIfUnmatched;
    private final Function<Artifact, String> artifactNameMapper;
    private final boolean allowOverwrite;
    private final HashSet<Path> writtenPaths;
    private final Path indexFile;
    private final IndexFileWriter indexFileWriter;
    private final StandardCopyOption[] copyFlags;
    private final boolean dryRun;

    /**
     * Creates a directory sink.
     *
     * @param output The output.
     * @param directory The directory, if not existing, will be created.
     * @param mode The accepting mode: copy, link or symlink.
     * @param artifactMatcher The matcher, that decides is this sink accepting artifact or not.
     * @param artifactNameMapper The artifact name mapper, that decides what file name will be of the artifact.
     * @param allowOverwrite Does sink allow overwrites. Tip: you usually do not want to allow, as that means you have
     *                       some mismatch in name mapping or alike.
     * @throws IOException In case of IO problem.
     */
    private DirectorySink(
            Output output,
            Path directory,
            Mode mode,
            Predicate<Artifact> artifactMatcher,
            boolean failIfUnmatched,
            Function<Artifact, String> artifactNameMapper,
            boolean allowOverwrite,
            boolean dryRun)
            throws IOException {
        this.output = requireNonNull(output, "output");
        this.directory = requireNonNull(directory, "directory").toAbsolutePath();
        this.mode = requireNonNull(mode, "mode");
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
        this.artifactNameMapper = requireNonNull(artifactNameMapper, "artifactNameMapper");
        this.allowOverwrite = allowOverwrite;
        this.writtenPaths = new HashSet<>();
        this.indexFile = directory.resolve(".index");
        this.indexFileWriter = new IndexFileWriter(indexFile, !directoryCreated, dryRun);
        this.copyFlags = allowOverwrite
                ? new StandardCopyOption[] {StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES}
                : new StandardCopyOption[] {StandardCopyOption.COPY_ATTRIBUTES};
        this.dryRun = dryRun;
    }

    public Path getDirectory() {
        return directory;
    }

    public Path getIndexFile() {
        return indexFile;
    }

    @Override
    public void accept(Artifact artifact) throws IOException {
        requireNonNull(artifact, "artifact");
        if (artifactMatcher.test(artifact)) {
            String name = artifactNameMapper.apply(artifact);
            Path target = directory.resolve(name).toAbsolutePath();
            if (!target.startsWith(directory)) {
                throw new IOException("Path escape prevented; check mappings");
            }
            if (!writtenPaths.add(target) && !allowOverwrite) {
                throw new IOException("Overwrite prevented; check mappings");
            }
            output.chatter("Accepting artifact {} -> ", artifact, target);
            indexFileWriter.write(artifact, name);
            Files.createDirectories(target.getParent());
            switch (mode) {
                case COPY:
                    if (!dryRun) {
                        Files.copy(artifact.getFile().toPath(), target, copyFlags);
                    }
                    break;
                case LINK:
                    if (!dryRun) {
                        Files.createLink(target, artifact.getFile().toPath());
                    }
                    break;
                case SYMLINK:
                    if (!dryRun) {
                        Files.createSymbolicLink(target, artifact.getFile().toPath());
                    }
                    break;
                default:
                    throw new IllegalArgumentException("unknown mode");
            }
        } else {
            if (failIfUnmatched) {
                throw new IllegalArgumentException("not matched");
            }
        }
    }

    @Override
    public void cleanup(Exception e) {
        indexFileWriter.fail();
        if (dryRun) {
            return;
        }
        writtenPaths.forEach(p -> {
            try {
                Files.deleteIfExists(p);
            } catch (IOException ex) {
                // ignore
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
    public void close() throws IOException {
        indexFileWriter.close();
    }
}
