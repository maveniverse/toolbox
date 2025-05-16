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
import eu.maveniverse.maven.toolbox.shared.internal.jdom.JDomExtensionsTransformer;
import eu.maveniverse.maven.toolbox.shared.internal.jdom.JDomTransformationContext;
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
 * Construction to accept collection of artifacts, and applies it to some extensions.xml based on provided transformations.
 */
public final class ExtensionsTransformerSink implements Artifacts.Sink {
    /**
     * Creates trivial "transform" sink, that accepts all artifacts and applies provided transformations to artifacts as-is.
     * If no extensions.xml file exists, will provide a plain/trivial "blank" file and work with that.
     */
    public static ExtensionsTransformerSink transform(Output output, Path extensions, ToolboxCommando.Op op)
            throws IOException {
        return transform(
                output,
                extensions,
                ExtensionsSuppliers::empty110,
                ArtifactMatcher.any(),
                ArtifactMapper.identity(),
                op);
    }

    /**
     * Creates "transform" sink, fully customizable.
     */
    public static ExtensionsTransformerSink transform(
            Output output,
            Path extensions,
            Supplier<String> extensionsSupplier,
            Predicate<Artifact> artifactMatcher,
            Function<Artifact, Artifact> artifactMapper,
            ToolboxCommando.Op op)
            throws IOException {
        return new ExtensionsTransformerSink(
                output, extensions, extensionsSupplier, artifactMatcher, artifactMapper, op);
    }

    private final Output output;
    private final Path extensions;
    private final Predicate<Artifact> artifactMatcher;
    private final Function<Artifact, Artifact> artifactMapper;
    private final Function<Artifact, Consumer<JDomTransformationContext.JdomExtensionsTransformationContext>>
            transformation;

    private final ArrayList<Consumer<JDomTransformationContext.JdomExtensionsTransformationContext>>
            applicableTransformations;

    /**
     * Creates a directory sink.
     *
     * @param output The output.
     * @param extensions The extensions.xml path, if not existing, will be created (as "blank").
     * @param extensionsSupplier Required, if path points to a non-existent extensions file.
     * @param artifactMatcher The artifact matcher.
     * @param artifactMapper The artifact mapper.
     * @param op The transformation op.
     * @throws IOException In case of IO problem.
     */
    private ExtensionsTransformerSink(
            Output output,
            Path extensions,
            Supplier<String> extensionsSupplier,
            Predicate<Artifact> artifactMatcher,
            Function<Artifact, Artifact> artifactMapper,
            ToolboxCommando.Op op)
            throws IOException {
        this.output = requireNonNull(output, "output");
        this.extensions = requireNonNull(extensions, "extensions").toAbsolutePath();
        if (!Files.isRegularFile(extensions)) {
            Files.createDirectories(extensions.getParent());
            Files.writeString(extensions, extensionsSupplier.get(), StandardCharsets.UTF_8);
        }
        this.artifactMatcher = requireNonNull(artifactMatcher, "artifactMatcher");
        this.artifactMapper = requireNonNull(artifactMapper, "artifactMapper");
        requireNonNull(op, "op");

        Function<Artifact, Consumer<JDomTransformationContext.JdomExtensionsTransformationContext>> tr = null;
        switch (op) {
            case UPSERT:
            case UPDATE:
                tr = JDomExtensionsTransformer.updateExtension(op == ToolboxCommando.Op.UPSERT);
                break;
            case DELETE:
                tr = JDomExtensionsTransformer.deleteExtension();
        }
        if (tr == null) {
            throw new IllegalArgumentException("Unknown op: " + op);
        }
        this.transformation = tr;
        this.applicableTransformations = new ArrayList<>();
    }

    public Path getExtensionsPath() {
        return extensions;
    }

    @Override
    public void accept(Artifact artifact) throws IOException {
        requireNonNull(artifact, "artifact");
        if (artifactMatcher.test(artifact)) {
            Consumer<JDomTransformationContext.JdomExtensionsTransformationContext> transformation =
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
            Files.deleteIfExists(extensions);
        } catch (IOException ex) {
            // ignore
        }
    }

    @Override
    public void close() throws IOException {
        new JDomExtensionsTransformer(extensions).apply(applicableTransformations);
    }
}
