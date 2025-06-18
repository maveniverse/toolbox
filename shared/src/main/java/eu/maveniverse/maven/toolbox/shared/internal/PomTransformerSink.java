/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.domtrip.Document;
import eu.maveniverse.maven.toolbox.shared.ArtifactMapper;
import eu.maveniverse.maven.toolbox.shared.ArtifactMatcher;
import eu.maveniverse.maven.toolbox.shared.FileUtils;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import eu.maveniverse.maven.toolbox.shared.internal.domtrip.SmartPomEditor;
import eu.maveniverse.maven.toolbox.shared.output.Output;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.eclipse.aether.artifact.Artifact;
import org.maveniverse.domtrip.maven.PomEditor;

/**
 * Construction to accept collection of artifacts, and applies it to some POM based on provided transformations.
 */
public final class PomTransformerSink implements Artifacts.Sink {
    /**
     * Creates trivial "transform" sink, that accepts all artifacts and applies provided transformations to artifacts as-is.
     * If no POM file exists, will provide a plain/trivial "blank" POM and work with that.
     */
    public static PomTransformerSink transform(
            Output output, Path pom, ToolboxCommando.PomOpSubject subject, ToolboxCommando.Op op) {
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
            ToolboxCommando.PomOpSubject subject,
            ToolboxCommando.Op op) {
        return new PomTransformerSink(output, pom, pomSupplier, artifactMatcher, artifactMapper, subject, op);
    }

    private final Output output;
    private final Path pom;
    private final Supplier<String> pomSupplier;
    private final Predicate<Artifact> artifactMatcher;
    private final Function<Artifact, Artifact> artifactMapper;
    private final Function<Artifact, Consumer<SmartPomEditor>> transformation;
    private final ArrayList<Consumer<SmartPomEditor>> applicableTransformations;

    /**
     * Creates a directory sink.
     *
     * @param output The output.
     * @param pom The POM path, if not existing, will be created (as "blank").
     * @param pomSupplier Required, if pom path points to a non-existent POM file.
     * @param artifactMatcher The artifact matcher.
     * @param artifactMapper The artifact mapper.
     * @param subject The transformation subject.
     * @param op The transformation op.
     */
    private PomTransformerSink(
            Output output,
            Path pom,
            Supplier<String> pomSupplier,
            Predicate<Artifact> artifactMatcher,
            Function<Artifact, Artifact> artifactMapper,
            ToolboxCommando.PomOpSubject subject,
            ToolboxCommando.Op op) {
        this.output = requireNonNull(output, "output");
        this.pom = requireNonNull(pom, "pom").toAbsolutePath();
        this.pomSupplier = requireNonNull(pomSupplier, "pomSupplier");
        this.artifactMatcher = requireNonNull(artifactMatcher, "artifactMatcher");
        this.artifactMapper = requireNonNull(artifactMapper, "artifactMapper");
        requireNonNull(subject, "subject");
        requireNonNull(op, "op");

        this.transformation = switch (op) {
            case UPSERT, UPDATE ->
                switch (subject) {
                    case MANAGED_PLUGINS -> a -> (e -> e.updateManagedPlugin(op == ToolboxCommando.Op.UPSERT, a));
                    case PLUGINS -> a -> (e -> e.updatePlugin(op == ToolboxCommando.Op.UPSERT, a));
                    case MANAGED_DEPENDENCIES ->
                        a -> (e -> e.updateManagedDependency(op == ToolboxCommando.Op.UPSERT, a));
                    case DEPENDENCIES -> a -> (e -> e.updateDependency(op == ToolboxCommando.Op.UPSERT, a));
                    case EXTENSIONS -> a -> (e -> e.updateExtension(op == ToolboxCommando.Op.UPSERT, a));
                };
            case DELETE ->
                switch (subject) {
                    case MANAGED_PLUGINS -> a -> (e -> e.deleteManagedPlugin(a));
                    case PLUGINS -> a -> (e -> e.deletePlugin(a));
                    case MANAGED_DEPENDENCIES -> a -> (e -> e.deleteManagedDependency(a));
                    case DEPENDENCIES -> a -> (e -> e.deleteDependency(a));
                    case EXTENSIONS -> a -> (e -> e.deleteExtension(a));
                };
        };
        this.applicableTransformations = new ArrayList<>();
    }

    public Path getPomPath() {
        return pom;
    }

    @Override
    public void accept(Artifact artifact) throws IOException {
        requireNonNull(artifact, "artifact");
        if (artifactMatcher.test(artifact)) {
            Consumer<SmartPomEditor> transformation = this.transformation.apply(artifactMapper.apply(artifact));
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
        Document document;
        if (!Files.isRegularFile(pom)) {
            Files.createDirectories(pom.getParent());
            document = Document.of(pomSupplier.get());
        } else {
            document = Document.of(pom);
        }
        SmartPomEditor editor = new SmartPomEditor(new PomEditor(document));
        try (FileUtils.CollocatedTempFile tempFile = FileUtils.newTempFile(pom, false)) {
            for (Consumer<SmartPomEditor> transformation : applicableTransformations) {
                transformation.accept(editor);
            }
            Files.writeString(tempFile.getPath(), document.toXml());
            tempFile.move();
        }
    }
}
