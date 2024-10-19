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
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3ReaderEx;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.util.artifact.SubArtifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Construction to supply collection of artifacts that are installed in given local repository.
 * <p>
 * Big fat note: "reverse engineering" file paths into GAVs is a risky business, and this code
 * assumes that Artifact classifiers does not contain {@code "."} (dot). In a moment your classifiers
 * do have dot character, figuring out extensions becomes much, much harder.
 */
public final class LocalRepositorySource implements Artifacts.Source {
    /**
     * Creates plain local repository source, that supplies all artifacts it has.
     */
    public static LocalRepositorySource local(Path directory) {
        return new LocalRepositorySource(directory);
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Path directory;
    private final MavenXpp3ReaderEx reader;

    /**
     * Creates a local repository source.
     *
     * @param directory The directory, if not existing, will be created.
     */
    private LocalRepositorySource(Path directory) {
        this.directory = requireNonNull(directory, "directory").toAbsolutePath();
        if (!Files.isDirectory(directory)) {
            throw new IllegalArgumentException("directory must exists");
        }
        this.reader = new MavenXpp3ReaderEx();
    }

    public Path getDirectory() {
        return directory;
    }

    @Override
    public Stream<Artifact> get() throws IOException {
        return poms().map(this::pomToArtifact)
                .filter(Objects::nonNull)
                .flatMap(this::collectPomArtifacts)
                .filter(Objects::nonNull);
    }

    private Stream<Path> poms() {
        ArrayList<Path> poms = new ArrayList<>();
        PathMatcher pomMatcher = directory.getFileSystem().getPathMatcher("glob:**/*.pom");
        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (pomMatcher.matches(file)) {
                        poms.add(file);
                    }
                    return super.visitFile(file, attrs);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return poms.stream();
    }

    private Artifact pomToArtifact(Path file) {
        try (InputStream is = Files.newInputStream(file)) {
            Model model = reader.read(is, false, null);
            String g = model.getGroupId();
            if (g == null || g.trim().isEmpty()) {
                g = model.getParent().getGroupId();
            }
            String a = model.getArtifactId();
            if (a == null || a.trim().isEmpty()) {
                a = model.getParent().getArtifactId();
            }
            String v = model.getVersion();
            if (v == null || v.trim().isEmpty()) {
                v = model.getParent().getVersion();
            }

            if (g != null && a != null && v != null) {
                return new DefaultArtifact(g, a, null, "pom", v).setFile(file.toFile());
            }
        } catch (IOException | XmlPullParserException e) {
            logger.info("Could not parse POM at {}", file, e);
        }
        return null;
    }

    private Stream<Artifact> collectPomArtifacts(Artifact pom) {
        ArrayList<Artifact> result = new ArrayList<>();
        result.add(pom);
        String pomFileName = pom.getFile().getName();
        String fileNamePrefix = pomFileName.substring(0, pom.getFile().getName().length() - 4);
        try (Stream<Path> files = Files.list(pom.getFile().toPath().getParent())) {
            files.filter(notItselfAndNotChecksum(pomFileName, fileNamePrefix)).forEach(p -> {
                String filename = p.getFileName().toString().substring(fileNamePrefix.length());
                if (filename.startsWith(".")) {
                    // no classifier, only ext
                    result.add(new SubArtifact(pom, null, filename.substring(1)).setFile(p.toFile()));
                } else if (filename.startsWith("-")) {
                    // classifier + ext // assuming classifier have no dot!
                    String classifier = filename.substring(1, filename.indexOf("."));
                    String extension = filename.substring(filename.indexOf(".") + 1);
                    result.add(new SubArtifact(pom, classifier, extension).setFile(p.toFile()));
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return result.stream();
    }

    private static Predicate<Path> notItselfAndNotChecksum(String fname, String prefix) {
        return p -> {
            String filename = p.getFileName().toString();
            return !fname.equals(filename)
                    && filename.startsWith(prefix)
                    && !filename.endsWith(".sha1")
                    && !filename.endsWith(".md5");
        };
    }
}
