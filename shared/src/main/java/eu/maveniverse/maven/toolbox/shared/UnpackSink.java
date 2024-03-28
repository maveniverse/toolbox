/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

import static java.util.Objects.requireNonNull;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.IOUtils;
import org.eclipse.aether.artifact.Artifact;

/**
 * Construction to accept collection of artifacts and unpack them.
 */
public final class UnpackSink implements ArtifactSink {
    /**
     * Creates plain "flat" directory where unpacks.
     */
    public static UnpackSink flat(Output output, Path path, Function<String, String> fileNameMapper)
            throws IOException {
        return new UnpackSink(
                output,
                path,
                ArtifactMatcher.unique(),
                false,
                a -> a,
                ArtifactNameMapper.ACVE(),
                fileNameMapper,
                false);
    }

    private final Output output;
    private final Path directory;
    private final boolean directoryCreated;
    private final Predicate<Artifact> artifactMatcher;
    private final boolean failIfUnmatched;
    private final Function<Artifact, Artifact> artifactMapper;
    private final Function<Artifact, String> artifactNameMapper;
    private final Function<String, String> fileNameMapper;
    private final boolean allowOverwrite;
    private final HashSet<Path> writtenPaths;
    /**
     * Creates a directory sink.
     *
     * @param output The output.
     * @param directory The directory, if not existing, will be created.
     * @param artifactMatcher The matcher, that decides is this sink accepting artifact or not.
     * @param artifactMapper The artifact mapper, that may re-map artifact.
     * @param artifactNameMapper The artifact name mapper, that decides what file name will be of the artifact.
     * @param fileNameMapper The file name mapper.
     * @param allowOverwrite Does sink allow overwrites. Tip: you usually do not want to allow, as that means you have
     *                       some mismatch in name mapping or alike.
     * @throws IOException In case of IO problem.
     */
    private UnpackSink(
            Output output,
            Path directory,
            Predicate<Artifact> artifactMatcher,
            boolean failIfUnmatched,
            Function<Artifact, Artifact> artifactMapper,
            Function<Artifact, String> artifactNameMapper,
            Function<String, String> fileNameMapper,
            boolean allowOverwrite)
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
        this.artifactNameMapper = requireNonNull(artifactNameMapper, "artifactNameMapper");
        this.fileNameMapper = requireNonNull(fileNameMapper, "fileNameMapper");
        this.allowOverwrite = allowOverwrite;
        this.writtenPaths = new HashSet<>();
    }

    public Path getDirectory() {
        return directory;
    }

    @Override
    public void accept(Artifact artifact) throws IOException {
        requireNonNull(artifact, "artifact");
        output.verbose("Accept artifact {}", artifact);
        if (artifactMatcher.test(artifact)) {
            output.verbose("  matched");
            String targetName = artifactNameMapper.apply(artifactMapper.apply(artifact));
            output.verbose("  mapped to name {}", targetName);
            Path target = directory.resolve(targetName).toAbsolutePath();
            if (!target.startsWith(directory)) {
                throw new IOException("Path escape prevented; check mappings");
            }
            if (!writtenPaths.add(target) && !allowOverwrite) {
                throw new IOException("Overwrite prevented; check mappings");
            }
            switch (artifact.getExtension()) {
                case "jar": {
                    unjar(target, artifact.getFile().toPath());
                    break;
                }
                case "zip": {
                    unzip(target, artifact.getFile().toPath());
                    break;
                }
                case "tar.gz": {
                    untar(
                            target,
                            new GzipCompressorInputStream(new BufferedInputStream(
                                    Files.newInputStream(artifact.getFile().toPath()))));
                    break;
                }
                case "tar.bz2": {
                    untar(
                            target,
                            new BZip2CompressorInputStream(new BufferedInputStream(
                                    Files.newInputStream(artifact.getFile().toPath()))));
                    break;
                }
                default:
                    throw new IllegalArgumentException("unknown archive");
            }
        } else {
            if (failIfUnmatched) {
                throw new IllegalArgumentException("not matched");
            } else {
                output.verbose("  not matched");
            }
        }
    }

    private void untar(Path target, InputStream input) throws IOException {
        try (TarArchiveInputStream tar = new TarArchiveInputStream(input)) {
            TarArchiveEntry entry;
            while ((entry = tar.getNextEntry()) != null) {
                if (!tar.canReadEntryData(entry)) {
                    output.warn("Cannot read entry {}", entry.getName());
                    continue;
                }
                Path f = mapToOutput(target, entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(f);
                } else {
                    Files.createDirectories(f.getParent());
                    try (OutputStream o = Files.newOutputStream(f)) {
                        IOUtils.copy(tar, o);
                        Files.setLastModifiedTime(f, entry.getLastModifiedTime());
                    }
                }
            }
        }
    }

    private void unzip(Path target, Path zipFile) throws IOException {
        try (ZipFile zip = ZipFile.builder().setFile(zipFile.toFile()).get()) {
            Enumeration<ZipArchiveEntry> zipArchiveEntryEnumeration = zip.getEntries();
            ZipArchiveEntry entry;
            while (zipArchiveEntryEnumeration.hasMoreElements()) {
                entry = zipArchiveEntryEnumeration.nextElement();
                if (!zip.canReadEntryData(entry)) {
                    output.warn("Cannot read entry {}", entry.getName());
                    continue;
                }
                Path f = mapToOutput(target, entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(f);
                } else {
                    Files.createDirectories(f.getParent());
                    try (OutputStream o = Files.newOutputStream(f)) {
                        IOUtils.copy(zip.getInputStream(entry), o);
                        Files.setLastModifiedTime(f, entry.getLastModifiedTime());
                    }
                }
            }
        }
    }

    private void unjar(Path target, Path jarFile) throws IOException {
        try (JarArchiveInputStream jar =
                new JarArchiveInputStream(new BufferedInputStream(Files.newInputStream(jarFile)))) {
            JarArchiveEntry entry;
            while ((entry = jar.getNextEntry()) != null) {
                if (!jar.canReadEntryData(entry)) {
                    output.warn("Cannot read entry {}", entry.getName());
                    continue;
                }
                Path f = mapToOutput(target, entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(f);
                } else {
                    Files.createDirectories(f.getParent());
                    try (OutputStream o = Files.newOutputStream(f)) {
                        IOUtils.copy(jar, o);
                        Files.setLastModifiedTime(f, entry.getLastModifiedTime());
                    }
                }
            }
        }
    }

    private Path mapToOutput(Path target, String entryName) throws IOException {
        Path f = target.resolve(fileNameMapper.apply(entryName)).toAbsolutePath();
        if (!f.startsWith(target)) {
            throw new IOException("Path escape prevented");
        }
        return f;
    }

    @Override
    public void cleanup(Exception e) {
        output.error("Cleaning up: {}", directory);
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
