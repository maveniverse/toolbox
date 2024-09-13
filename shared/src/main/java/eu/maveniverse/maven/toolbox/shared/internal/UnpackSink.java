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
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Construction to accept collection of artifacts and unpack them.
 */
public final class UnpackSink extends ArtifactSink {
    /**
     * Creates plain unpack sink where unpacking happens according to supplied parameters.
     *
     * @param path The root where unpack happens.
     * @param artifactRootMapper The artifact root mapper, that decides where is root of unpacking for given artifact.
     *                           To achieve "overlay", one can use {@code fixed(.)} mapper that will map roots into
     *                           root of {@code path} parameter.
     * @param allowEntryOverwrite Does this sink allow entry overlap (among unpacked archives) or not?
     */
    public static UnpackSink unpack(
            Path path, Function<Artifact, String> artifactRootMapper, boolean allowEntryOverwrite) throws IOException {
        return new UnpackSink(
                path,
                ArtifactMatcher.unique(),
                false,
                a -> a,
                artifactRootMapper,
                Function.identity(),
                true,
                allowEntryOverwrite);
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Path directory;
    private final boolean directoryCreated;
    private final Predicate<Artifact> artifactMatcher;
    private final boolean failIfUnmatched;
    private final Function<Artifact, Artifact> artifactMapper;
    private final Function<Artifact, String> artifactRootMapper;
    private final Function<String, String> fileNameMapper;
    private final boolean allowRootOverwrite;
    private final boolean allowEntryOverwrite;
    private final HashSet<Path> writtenPaths;
    /**
     * Creates a directory sink.
     *
     * @param directory The directory, if not existing, will be created.
     * @param artifactMatcher The matcher, that decides is this sink accepting artifact or not.
     * @param artifactMapper The artifact mapper, that may re-map artifact.
     * @param artifactRootMapper The artifact root mapper, that decides where is root of unpacking for given artifact.
     * @param fileNameMapper The file name mapper.
     * @param allowRootOverwrite Does sink allow use of same roots for unpack operations.
     * @param allowEntryOverwrite Does sink allow unpacked entry overwrites. Tip: you usually do not want to allow,
     *                            as that means you have some overlap in unpacked archives.
     * @throws IOException In case of IO problem.
     */
    private UnpackSink(
            Path directory,
            Predicate<Artifact> artifactMatcher,
            boolean failIfUnmatched,
            Function<Artifact, Artifact> artifactMapper,
            Function<Artifact, String> artifactRootMapper,
            Function<String, String> fileNameMapper,
            boolean allowRootOverwrite,
            boolean allowEntryOverwrite)
            throws IOException {
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
        this.fileNameMapper = requireNonNull(fileNameMapper, "fileNameMapper");
        this.allowRootOverwrite = allowRootOverwrite;
        this.allowEntryOverwrite = allowEntryOverwrite;
        this.writtenPaths = new HashSet<>();
    }

    public Path getDirectory() {
        return directory;
    }

    @Override
    public void accept(Artifact artifact) throws IOException {
        requireNonNull(artifact, "artifact");
        logger.debug("Accept artifact {}", artifact);
        if (artifactMatcher.test(artifact)) {
            logger.debug("  matched");
            String targetName = artifactRootMapper.apply(artifactMapper.apply(artifact));
            logger.debug("  mapped to name {}", targetName);
            Path target = directory.resolve(targetName).toAbsolutePath();
            if (!target.startsWith(directory)) {
                throw new IOException("Path escape prevented; check mappings");
            }
            if (!writtenPaths.add(target) && !allowRootOverwrite) {
                throw new IOException("Root overwrite prevented; check mappings");
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
            }
        }
    }

    private void untar(Path target, InputStream input) throws IOException {
        try (TarArchiveInputStream tar = new TarArchiveInputStream(input)) {
            TarArchiveEntry entry;
            while ((entry = tar.getNextEntry()) != null) {
                if (!tar.canReadEntryData(entry)) {
                    logger.warn("Cannot read entry {}", entry.getName());
                    continue;
                }
                Path f = mapToOutput(target, entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(f);
                } else {
                    Files.createDirectories(f.getParent());
                    mayCopy(f, tar, entry.getLastModifiedTime());
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
                    logger.warn("Cannot read entry {}", entry.getName());
                    continue;
                }
                Path f = mapToOutput(target, entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(f);
                } else {
                    Files.createDirectories(f.getParent());
                    mayCopy(f, zip.getInputStream(entry), entry.getLastModifiedTime());
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
                    logger.warn("Cannot read entry {}", entry.getName());
                    continue;
                }
                Path f = mapToOutput(target, entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(f);
                } else {
                    Files.createDirectories(f.getParent());
                    mayCopy(f, jar, entry.getLastModifiedTime());
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

    private void mayCopy(Path target, InputStream inputStream, FileTime fileTime) throws IOException {
        if (Files.exists(target) && !allowEntryOverwrite) {
            throw new IOException("Entry overwrite prevented; overlap in archives");
        }
        try (OutputStream o = Files.newOutputStream(target)) {
            IOUtils.copy(inputStream, o);
            if (fileTime != null) {
                Files.setLastModifiedTime(target, fileTime);
            }
        }
    }

    @Override
    public void cleanup(Exception e) {
        writtenPaths.forEach(p -> {
            try (Stream<Path> stream = Files.walk(p).sorted(Comparator.reverseOrder())) {
                stream.forEach(f -> {
                    try {
                        Files.delete(f);
                    } catch (IOException ioex) {
                        logger.warn("Could not delete {}", p, ioex);
                    }
                });
            } catch (IOException ioex) {
                logger.warn("Could not walk {}", p, ioex);
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
