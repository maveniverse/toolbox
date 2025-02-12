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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.eclipse.aether.artifact.Artifact;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.located.LocatedJDOMFactory;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

/**
 * Construction to accept collection of artifacts, and applies it to some POM based on provided transformations.
 */
public final class PomTransformerSink implements Artifacts.Sink {
    /**
     * A transformation to apply.
     */
    public interface Transformation extends Consumer<TransformationContext> {}

    /**
     * The transformation context.
     */
    public interface TransformationContext {
        Document getDocument();
    }

    /**
     * Removes empty remnant tags, like {@code <plugins />}.
     */
    private final Transformation removeEmptyElements = ctx -> {};

    public static Function<Artifact, Consumer<TransformationContext>> updateManagedPlugin(boolean upsert) {
        return a -> context -> {
            throw new RuntimeException("not implemented");
        };
    }

    public static Function<Artifact, Consumer<TransformationContext>> deleteManagedPlugin() {
        return a -> context -> {
            throw new RuntimeException("not implemented");
        };
    }

    public static Function<Artifact, Consumer<TransformationContext>> updatePlugin(boolean upsert) {
        return a -> context -> {
            throw new RuntimeException("not implemented");
        };
    }

    public static Function<Artifact, Consumer<TransformationContext>> deletePlugin() {
        return a -> context -> {
            throw new RuntimeException("not implemented");
        };
    }

    public static Function<Artifact, Consumer<TransformationContext>> updateManagedDependency(boolean upsert) {
        return a -> context -> {
            throw new RuntimeException("not implemented");
        };
    }

    public static Function<Artifact, Consumer<TransformationContext>> deleteManagedDependency() {
        return a -> context -> {
            throw new RuntimeException("not implemented");
        };
    }

    public static Function<Artifact, Consumer<TransformationContext>> updateDependency(boolean upsert) {
        return a -> context -> {
            throw new RuntimeException("not implemented");
        };
    }

    public static Function<Artifact, Consumer<TransformationContext>> deleteDependency() {
        return a -> context -> {
            throw new RuntimeException("not implemented");
        };
    }

    /**
     * Creates trivial "transform" sink, that accepts all artifacts and applies provided transformations to artifacts as-is.
     * If no POM file exists, will provide a plain/trivial "blank" POM.
     */
    public static PomTransformerSink transform(
            Output output, Path pom, Function<Artifact, Consumer<TransformationContext>> transformations)
            throws IOException {
        return transform(
                output, pom, () -> BLANK_POM, ArtifactMatcher.any(), ArtifactMapper.identity(), transformations);
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
            Function<Artifact, Consumer<TransformationContext>> transformations)
            throws IOException {
        return new PomTransformerSink(output, pom, pomSupplier, artifactMatcher, artifactMapper, transformations);
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
    private final Predicate<Artifact> artifactMatcher;
    private final Function<Artifact, Artifact> artifactMapper;
    private final Function<Artifact, Consumer<TransformationContext>> transformations;

    private final ArrayList<Consumer<TransformationContext>> applicableTransformations;

    /**
     * Creates a directory sink.
     *
     * @param output The output.
     * @param pom The POM path, if not existing, will be created (as "blank").
     * @param blankPomSupplier Required, if pom path points to a non-existent POM file.
     * @param artifactMatcher The artifact matcher.
     * @param artifactMapper The artifact mapper.
     * @param transformations The transformations to apply.
     * @throws IOException In case of IO problem.
     */
    private PomTransformerSink(
            Output output,
            Path pom,
            Supplier<String> blankPomSupplier,
            Predicate<Artifact> artifactMatcher,
            Function<Artifact, Artifact> artifactMapper,
            Function<Artifact, Consumer<TransformationContext>> transformations)
            throws IOException {
        this.output = requireNonNull(output, "output");
        this.pom = requireNonNull(pom, "pom").toAbsolutePath();
        if (!Files.isRegularFile(pom)) {
            Files.createDirectories(pom.getParent());
            Files.writeString(pom, blankPomSupplier.get(), StandardCharsets.UTF_8);
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
            Consumer<TransformationContext> transformation = transformations.apply(artifactMapper.apply(artifact));
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
        if (!applicableTransformations.isEmpty()) {
            SAXBuilder builder = new SAXBuilder();
            builder.setJDOMFactory(new LocatedJDOMFactory());
            Document document;
            try (InputStream inputStream = Files.newInputStream(pom)) {
                document = builder.build(inputStream);
            } catch (JDOMException e) {
                throw new IOException(e);
            }
            TransformationContext context = new TransformationContext() {
                @Override
                public Document getDocument() {
                    return document;
                }
            };
            for (Consumer<TransformationContext> transformation : applicableTransformations) {
                transformation.accept(context);
            }

            XMLOutputter out = new XMLOutputter(Format.getRawFormat());
            try (OutputStream outputStream = Files.newOutputStream(pom)) {
                out.output(document, outputStream);
                outputStream.flush();
            }
        }
    }
}
