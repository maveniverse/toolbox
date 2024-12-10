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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.eclipse.aether.artifact.Artifact;
import org.l2x6.pom.tuner.PomTransformer;
import org.l2x6.pom.tuner.model.Ga;
import org.l2x6.pom.tuner.model.Gavtcs;
import org.w3c.dom.Document;

/**
 * Construction to accept collection of artifacts, and applies it to some POM based on provided transformations.
 */
public final class PomTransformerSink implements Artifacts.Sink {
    public static Function<Artifact, PomTransformer.Transformation> updateManagedPlugin(boolean upsert) {
        return a -> (Document document, PomTransformer.TransformationContext context) -> {
            List<Ga> gas = List.of(Ga.of(a.getGroupId(), a.getArtifactId()));
            final PomTransformer.ContainerElement targetSection = context.getContainerElement("build")
                    .flatMap(e -> e.getChildContainerElement("pluginManagement"))
                    .flatMap(e -> e.getChildContainerElement("plugins"))
                    .orElse(null);

            boolean present = false;
            if (targetSection != null) {
                for (PomTransformer.ContainerElement element : targetSection.childElements()) {
                    final Ga ga = element.asGavtcs().toGa();
                    if (gas.contains(ga)) {
                        present = true;
                        Optional<PomTransformer.ContainerElement> versionNode = element.childElementsStream()
                                .filter(ch -> "version".equals(ch.getNode().getLocalName()))
                                .findFirst();
                        if (versionNode.isEmpty()) {
                            // nothing to do; is managed
                        } else {
                            String value = versionNode.orElseThrow().getNode().getTextContent();
                            if (value != null && value.startsWith("${") && value.endsWith("}")) {
                                // nothing to do, is expression
                                String versionProperty = value.substring(2, value.length() - 1);
                                PomTransformer.Transformation.addOrSetProperty(versionProperty, a.getVersion())
                                        .perform(document, context);
                            } else {
                                versionNode.orElseThrow().getNode().setTextContent(a.getVersion());
                            }
                        }
                    }
                }
            }
            if (upsert && !present) {
                context.getOrAddContainerElement("build")
                        .getOrAddChildContainerElement("pluginManagement")
                        .getOrAddChildContainerElement("plugins")
                        .addGavtcs(new Gavtcs(a.getGroupId(), a.getArtifactId(), a.getVersion()));
            }
        };
    }

    public static Function<Artifact, PomTransformer.Transformation> deleteManagedPlugin() {
        // TODO:
        // return a -> PomTransformer.Transformation.removeManagedPlugins(null, true, true,
        // Gavtcs.equalGroupIdAndArtifactId(a.getGroupId(), a.getArtifactId()));
        throw new RuntimeException("not implemented");
    }

    public static Function<Artifact, PomTransformer.Transformation> updatePlugin(boolean upsert) {
        return a -> (Document document, PomTransformer.TransformationContext context) -> {
            List<Ga> gas = List.of(Ga.of(a.getGroupId(), a.getArtifactId()));
            final PomTransformer.ContainerElement targetSection = context.getContainerElement("build")
                    .flatMap(e -> e.getChildContainerElement("plugins"))
                    .orElse(null);

            boolean present = false;
            if (targetSection != null) {
                for (PomTransformer.ContainerElement element : targetSection.childElements()) {
                    final Ga ga = element.asGavtcs().toGa();
                    if (gas.contains(ga)) {
                        present = true;
                        Optional<PomTransformer.ContainerElement> versionNode = element.childElementsStream()
                                .filter(ch -> "version".equals(ch.getNode().getLocalName()))
                                .findFirst();
                        if (versionNode.isEmpty()) {
                            // nothing to do; is managed
                        } else {
                            String value = versionNode.orElseThrow().getNode().getTextContent();
                            if (value != null && value.startsWith("${") && value.endsWith("}")) {
                                // nothing to do, is expression
                                String versionProperty = value.substring(2, value.length() - 1);
                                PomTransformer.Transformation.addOrSetProperty(versionProperty, a.getVersion())
                                        .perform(document, context);
                            } else {
                                versionNode.orElseThrow().getNode().setTextContent(a.getVersion());
                            }
                        }
                    }
                }
            }
            if (upsert && !present) {
                context.getOrAddContainerElement("build")
                        .getOrAddChildContainerElement("plugins")
                        .addGavtcs(new Gavtcs(a.getGroupId(), a.getArtifactId(), a.getVersion()));
            }
        };
    }

    public static Function<Artifact, PomTransformer.Transformation> deletePlugin() {
        return a -> PomTransformer.Transformation.removePlugins(
                null, true, true, Gavtcs.equalGroupIdAndArtifactId(a.getGroupId(), a.getArtifactId()));
    }

    public static Function<Artifact, PomTransformer.Transformation> updateManagedDependency(boolean upsert) {
        return a -> (Document document, PomTransformer.TransformationContext context) -> {
            List<Ga> gas = List.of(Ga.of(a.getGroupId(), a.getArtifactId()));
            final PomTransformer.ContainerElement targetSection = context.getContainerElement("dependencyManagement")
                    .flatMap(e -> e.getChildContainerElement("dependencies"))
                    .orElse(null);

            boolean present = false;
            if (targetSection != null) {
                for (PomTransformer.ContainerElement element : targetSection.childElements()) {
                    final Ga ga = element.asGavtcs().toGa();
                    if (gas.contains(ga)) {
                        present = true;
                        Optional<PomTransformer.ContainerElement> versionNode = element.childElementsStream()
                                .filter(ch -> "version".equals(ch.getNode().getLocalName()))
                                .findFirst();
                        if (versionNode.isEmpty()) {
                            // nothing to do; is managed
                        } else {
                            String value = versionNode.orElseThrow().getNode().getTextContent();
                            if (value != null && value.startsWith("${") && value.endsWith("}")) {
                                // nothing to do, is expression
                                String versionProperty = value.substring(2, value.length() - 1);
                                PomTransformer.Transformation.addOrSetProperty(versionProperty, a.getVersion())
                                        .perform(document, context);
                            } else {
                                versionNode.orElseThrow().getNode().setTextContent(a.getVersion());
                            }
                        }
                    }
                }
            }
            if (upsert && !present) {
                context.getOrAddContainerElement("dependencyManagement")
                        .getOrAddChildContainerElement("dependencies")
                        .addGavtcs(new Gavtcs(a.getGroupId(), a.getArtifactId(), a.getVersion()));
            }
        };
    }

    public static Function<Artifact, PomTransformer.Transformation> deleteManagedDependency() {
        return a -> PomTransformer.Transformation.removeManagedDependencies(
                null, true, true, Gavtcs.equalGroupIdAndArtifactId(a.getGroupId(), a.getArtifactId()));
    }

    public static Function<Artifact, PomTransformer.Transformation> updateDependency(boolean upsert) {
        return a -> (Document document, PomTransformer.TransformationContext context) -> {
            List<Ga> gas = List.of(Ga.of(a.getGroupId(), a.getArtifactId()));
            final PomTransformer.ContainerElement targetSection =
                    context.getContainerElement("dependencies").orElse(null);

            boolean present = false;
            if (targetSection != null) {
                for (PomTransformer.ContainerElement element : targetSection.childElements()) {
                    final Ga ga = element.asGavtcs().toGa();
                    if (gas.contains(ga)) {
                        present = true;
                        Optional<PomTransformer.ContainerElement> versionNode = element.childElementsStream()
                                .filter(ch -> "version".equals(ch.getNode().getLocalName()))
                                .findFirst();
                        if (versionNode.isEmpty()) {
                            // nothing to do; is managed
                        } else {
                            String value = versionNode.orElseThrow().getNode().getTextContent();
                            if (value != null && value.startsWith("${") && value.endsWith("}")) {
                                // nothing to do, is expression
                                String versionProperty = value.substring(2, value.length() - 1);
                                PomTransformer.Transformation.addOrSetProperty(versionProperty, a.getVersion())
                                        .perform(document, context);
                            } else {
                                versionNode.orElseThrow().getNode().setTextContent(a.getVersion());
                            }
                        }
                    }
                }
            }
            if (upsert && !present) {
                context.getOrAddContainerElement("dependencies")
                        .addGavtcs(new Gavtcs(a.getGroupId(), a.getArtifactId(), a.getVersion()));
            }
        };
    }

    public static Function<Artifact, PomTransformer.Transformation> deleteDependency() {
        return a -> PomTransformer.Transformation.removeDependencies(
                null, true, true, Gavtcs.equalGroupIdAndArtifactId(a.getGroupId(), a.getArtifactId()));
    }

    /**
     * Creates trivial "transform" sink, that accepts all artifacts and applies provided transformations to artifacts as-is.
     * If no POM file exists, will provide a plain/trivial "blank" POM.
     */
    public static PomTransformerSink transform(
            Output output, Path pom, Function<Artifact, PomTransformer.Transformation> transformations)
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
            Function<Artifact, PomTransformer.Transformation> transformations)
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
    private final Function<Artifact, PomTransformer.Transformation> transformations;

    private final ArrayList<PomTransformer.Transformation> applicableTransformations;

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
            Function<Artifact, PomTransformer.Transformation> transformations)
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
        } catch (IOException ex) {
            // ignore
        }
    }

    @Override
    public void close() throws IOException {
        new PomTransformer(pom, StandardCharsets.UTF_8, PomTransformer.SimpleElementWhitespace.AUTODETECT_PREFER_EMPTY)
                .transform(applicableTransformations);
    }
}
