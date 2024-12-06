/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.toolbox.shared.ArtifactMapper;
import eu.maveniverse.maven.toolbox.shared.ArtifactMatcher;
import eu.maveniverse.maven.toolbox.shared.output.Output;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.function.Predicate;
import org.eclipse.aether.artifact.Artifact;
import org.l2x6.pom.tuner.PomTransformer;

/**
 * Construction to accept collection of artifacts, and applies it to some POM based on provided transformations.
 */
public final class PomTransformerSink implements Artifacts.Sink {
    /**
     * Transformation to add received artifact as managed plugin.
     */
    public static Function<Artifact, PomTransformer.Transformation> addManagedPlugin() {
        return a -> PomTransformer.Transformation.addManagedPlugin(a.getGroupId(), a.getArtifactId(), a.getVersion());
    }

    /**
     * Creates "transform" sink, that accepts all artifacts and applies provided transformations to artifacts as-is.
     */
    public static PomTransformerSink transform(
            Output output, Path pom, Function<Artifact, PomTransformer.Transformation> transformations)
            throws IOException {
        return new PomTransformerSink(output, pom, ArtifactMatcher.any(), ArtifactMapper.identity(), transformations);
    }

    private static final String BLANK_POM = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
            + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
            + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
            + "    <modelVersion>4.0.0</modelVersion>\n" //
            + "    <groupId>org.acme</groupId>\n" //
            + "    <artifactId>pom</artifactId>\n" //
            + "    <version>1.0-SNAPSHOT</version>\n" //
            + "    <packaging>pom</packaging>\n" //
            + "</project>\n";

    private final Output output;
    private final Path pom;
    private final boolean existingPom;
    private final Predicate<Artifact> artifactMatcher;
    private final Function<Artifact, Artifact> artifactMapper;
    private final Function<Artifact, PomTransformer.Transformation> transformations;

    private final ArrayList<PomTransformer.Transformation> applicableTransformations;

    /**
     * Creates a directory sink.
     *
     * @param output The output.
     * @param pom The POM path, if not existing, will be created (as "blank").
     * @param artifactMatcher The artifact matcher.
     * @param artifactMapper The artifact mapper.
     * @param transformations The transformations to apply.
     * @throws IOException In case of IO problem.
     */
    private PomTransformerSink(
            Output output,
            Path pom,
            Predicate<Artifact> artifactMatcher,
            Function<Artifact, Artifact> artifactMapper,
            Function<Artifact, PomTransformer.Transformation> transformations)
            throws IOException {
        this.output = requireNonNull(output, "output");
        this.pom = requireNonNull(pom, "pom").toAbsolutePath();
        if (Files.isRegularFile(pom)) {
            existingPom = true;
            Files.copy(
                    pom,
                    pom.getParent().resolve(getPomPath().getFileName() + ".bak"),
                    StandardCopyOption.REPLACE_EXISTING);
        } else {
            Files.createDirectories(pom.getParent());
            Files.writeString(pom, BLANK_POM);
            existingPom = false;
        }
        this.artifactMatcher = requireNonNull(artifactMatcher, "artifactMatcher");
        this.artifactMapper = requireNonNull(artifactMapper, "artifactMapper");
        this.transformations = requireNonNull(transformations, "transformations");

        this.applicableTransformations = new ArrayList<>();
    }

    public Path getPomPath() {
        return pom;
    }

    @Override
    public void accept(Artifact artifact) throws IOException {
        requireNonNull(artifact, "artifact");
        if (artifactMatcher.test(artifact)) {
            PomTransformer.Transformation transformation = transformations.apply(artifactMapper.apply(artifact));
            if (transformation != null) {
                output.chatter("Accepted {}", artifact);
                applicableTransformations.add(transformation);
            }
        }
    }

    @Override
    public void cleanup(Exception e) {
        try {
            Files.deleteIfExists(pom);
            if (existingPom) {
                // return existing one backup
                Files.copy(
                        pom.getParent().resolve(getPomPath().getFileName() + ".bak"),
                        pom,
                        StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            // ignore
        }
    }

    @Override
    public void close() throws IOException {
        new PomTransformer(pom, StandardCharsets.UTF_8, PomTransformer.SimpleElementWhitespace.EMPTY)
                .transform(applicableTransformations);
    }
}
