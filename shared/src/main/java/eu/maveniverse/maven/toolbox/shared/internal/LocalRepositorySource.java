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
import java.util.stream.Stream;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3ReaderEx;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

/**
 * Construction to supply collection of artifacts that are installed in given local repository.
 */
public final class LocalRepositorySource implements Artifacts.Source {
    /**
     * Creates plain local repository source, that supplies all artifacts it has.
     */
    public static LocalRepositorySource local(Path directory) {
        return new LocalRepositorySource(directory);
    }

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
            // silently skip
        }
        return null;
    }

    private Stream<Artifact> collectPomArtifacts(Artifact pom) {
        ArrayList<Artifact> result = new ArrayList<>();
        result.add(pom);
        // TODO: collect all files from the parent of pom file
        return result.stream();
    }
}
