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
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import eu.maveniverse.maven.toolbox.shared.internal.jdom.JDomPomTransformer;
import eu.maveniverse.maven.toolbox.shared.output.Output;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.eclipse.aether.artifact.Artifact;

/**
 * Construction to accept collection of artifacts, and applies it to some POM based on provided transformations.
 */
public final class PomTransformerSink implements Artifacts.Sink {
    /**
     * Creates trivial "transform" sink, that accepts all artifacts and applies provided transformations to artifacts as-is.
     * If no POM file exists, will provide a plain/trivial "blank" POM.
     */
    public static PomTransformerSink transform(
            Output output, Path pom, ToolboxCommando.OpSubject subject, ToolboxCommando.Op op) throws IOException {
        return transform(
                output,
                pom,
                () -> PomSuppliers.empty400("org.acme", "acme", "1.0.0-SNAPSHOT"),
                ArtifactMatcher.any(),
                ArtifactMapper.identity(),
                subject,
                op);
    }

    /**
     * Creates "transform" sink, fully customizable.
     */
    public static PomTransformerSink transform(
            Output output,
            Path pom,
            Supplier<String> pomSupplier,
            Predicate<Artifact> artifactMatcher,
            Function<Artifact, Artifact> artifactMapper,
            ToolboxCommando.OpSubject subject,
            ToolboxCommando.Op op)
            throws IOException {
        return new PomTransformerSink(output, pom, pomSupplier, artifactMatcher, artifactMapper, subject, op);
    }

    private static final String BLANK_POM = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
            + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
            + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
            + "    <modelVersion>4.0.0</modelVersion>\n" //
            + "\n" //
            + "    <groupId>org.acme</groupId>\n" //
            + "    <artifactId>pom</artifactId>\n" //
            + "    <version>1.0-SNAPSHOT</version>\n" //
            + "    <packaging>pom</packaging>\n" //
            + "</project>\n";

    private final Output output;
    private final Path pom;
    private final Predicate<Artifact> artifactMatcher;
    private final Function<Artifact, Artifact> artifactMapper;
    private final Function<Artifact, Consumer<JDomPomTransformer.TransformationContext>> transformation;

    private final ArrayList<Consumer<JDomPomTransformer.TransformationContext>> applicableTransformations;

    /**
     * Creates a directory sink.
     *
     * @param output The output.
     * @param pom The POM path, if not existing, will be created (as "blank").
     * @param blankPomSupplier Required, if pom path points to a non-existent POM file.
     * @param artifactMatcher The artifact matcher.
     * @param artifactMapper The artifact mapper.
     * @param subject The transformation subject.
     * @param op The transformation op.
     * @throws IOException In case of IO problem.
     */
    private PomTransformerSink(
            Output output,
            Path pom,
            Supplier<String> blankPomSupplier,
            Predicate<Artifact> artifactMatcher,
            Function<Artifact, Artifact> artifactMapper,
            ToolboxCommando.OpSubject subject,
            ToolboxCommando.Op op)
            throws IOException {
        this.output = requireNonNull(output, "output");
        this.pom = requireNonNull(pom, "pom").toAbsolutePath();
        if (!Files.isRegularFile(pom)) {
            Files.createDirectories(pom.getParent());
            Files.writeString(pom, blankPomSupplier.get(), StandardCharsets.UTF_8);
        }
        this.artifactMatcher = requireNonNull(artifactMatcher, "artifactMatcher");
        this.artifactMapper = requireNonNull(artifactMapper, "artifactMapper");
        requireNonNull(subject, "subject");
        requireNonNull(op, "op");

        Function<Artifact, Consumer<JDomPomTransformer.TransformationContext>> tr = null;
        switch (op) {
            case UPSERT:
            case UPDATE:
                switch (subject) {
                    case MANAGED_PLUGINS:
                        tr = JDomPomTransformer.updateManagedPlugin(op == ToolboxCommando.Op.UPSERT);
                        break;
                    case PLUGINS:
                        tr = JDomPomTransformer.updatePlugin(op == ToolboxCommando.Op.UPSERT);
                        break;
                    case MANAGED_DEPENDENCIES:
                        tr = JDomPomTransformer.updateManagedDependency(op == ToolboxCommando.Op.UPSERT);
                        break;
                    case DEPENDENCIES:
                        tr = JDomPomTransformer.updateDependency(op == ToolboxCommando.Op.UPSERT);
                        break;
                }
                break;
            case DELETE:
                switch (subject) {
                    case MANAGED_PLUGINS:
                        tr = JDomPomTransformer.deleteManagedPlugin();
                        break;
                    case PLUGINS:
                        tr = JDomPomTransformer.deletePlugin();
                        break;
                    case MANAGED_DEPENDENCIES:
                        tr = JDomPomTransformer.deleteManagedDependency();
                        break;
                    case DEPENDENCIES:
                        tr = JDomPomTransformer.deleteDependency();
                        break;
                }
        }
        if (tr == null) {
            throw new IllegalArgumentException("Unknown subject: " + subject + " and/or op: " + op);
        }
        this.transformation = tr;
        this.applicableTransformations = new ArrayList<>();
    }

    public Path getPomPath() {
        return pom;
    }

    @Override
    public void accept(Artifact artifact) throws IOException {
        requireNonNull(artifact, "artifact");
        if (artifactMatcher.test(artifact)) {
            Consumer<JDomPomTransformer.TransformationContext> transformation =
                    this.transformation.apply(artifactMapper.apply(artifact));
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
        } catch (IOException ex) {
            // ignore
        }
    }

    @Override
    public void close() throws IOException {
        new JDomPomTransformer().apply(pom, applicableTransformations);
    }
}
