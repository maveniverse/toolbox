/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static eu.maveniverse.maven.toolbox.shared.internal.domtrip.DOMTripUtils.toDomTrip;
import static java.util.Objects.requireNonNull;

import eu.maveniverse.domtrip.Document;
import eu.maveniverse.domtrip.maven.PomEditor;
import eu.maveniverse.maven.toolbox.shared.ArtifactMapper;
import eu.maveniverse.maven.toolbox.shared.ArtifactMatcher;
import eu.maveniverse.maven.toolbox.shared.FileUtils;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
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
                op,
                null);
    }

    /**
     * Creates a profile-scoped "transform" sink. All plugin/managed-plugin operations will be applied
     * within the specified profile's build section instead of the main build.
     *
     * <p>For non-plugin subjects ({@code DEPENDENCIES}, {@code MANAGED_DEPENDENCIES}, {@code EXTENSIONS})
     * the {@code profileId} is ignored and the operation targets the main build, preserving existing behaviour.</p>
     *
     * @param output    the output
     * @param pom       the POM path
     * @param subject   the transformation subject
     * @param op        the operation
     * @param profileId the profile id to scope plugin operations to; {@code null} targets the main build
     */
    public static PomTransformerSink transform(
            Output output, Path pom, ToolboxCommando.PomOpSubject subject, ToolboxCommando.Op op, String profileId) {
        return transform(
                output,
                pom,
                () -> PomSuppliers.empty400("org.acme", "acme", "1.0.0-SNAPSHOT"),
                ArtifactMatcher.any(),
                ArtifactMapper.identity(),
                subject,
                op,
                profileId);
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
        return new PomTransformerSink(output, pom, pomSupplier, artifactMatcher, artifactMapper, subject, op, null);
    }

    /**
     * Creates "transform" sink, fully customizable, with optional profile scope.
     */
    public static PomTransformerSink transform(
            Output output,
            Path pom,
            Supplier<String> pomSupplier,
            Predicate<Artifact> artifactMatcher,
            Function<Artifact, Artifact> artifactMapper,
            ToolboxCommando.PomOpSubject subject,
            ToolboxCommando.Op op,
            String profileId) {
        return new PomTransformerSink(
                output, pom, pomSupplier, artifactMatcher, artifactMapper, subject, op, profileId);
    }

    private final Output output;
    private final Path pom;
    private final Supplier<String> pomSupplier;
    private final Predicate<Artifact> artifactMatcher;
    private final Function<Artifact, Artifact> artifactMapper;
    private final Function<Artifact, Consumer<PomEditor>> transformation;
    private final ArrayList<Consumer<PomEditor>> applicableTransformations;

    /**
     * Creates a POM transformer sink.
     *
     * @param output         The output.
     * @param pom            The POM path, if not existing, will be created (as "blank").
     * @param pomSupplier    Required, if pom path points to a non-existent POM file.
     * @param artifactMatcher The artifact matcher.
     * @param artifactMapper  The artifact mapper.
     * @param subject         The transformation subject.
     * @param op              The transformation op.
     * @param profileId       If non-null, scope plugin/managed-plugin operations to this profile.
     */
    private PomTransformerSink(
            Output output,
            Path pom,
            Supplier<String> pomSupplier,
            Predicate<Artifact> artifactMatcher,
            Function<Artifact, Artifact> artifactMapper,
            ToolboxCommando.PomOpSubject subject,
            ToolboxCommando.Op op,
            String profileId) {
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
                    case MANAGED_PLUGINS ->
                        profileId != null
                                ? a -> (e -> e.plugins()
                                        .forProfile(profileId)
                                        .updateManagedPlugin(op == ToolboxCommando.Op.UPSERT, toDomTrip(a)))
                                : a -> (e ->
                                        e.plugins().updateManagedPlugin(op == ToolboxCommando.Op.UPSERT, toDomTrip(a)));
                    case PLUGINS ->
                        profileId != null
                                ? a -> (e -> e.plugins()
                                        .forProfile(profileId)
                                        .updatePlugin(op == ToolboxCommando.Op.UPSERT, toDomTrip(a)))
                                : a -> (e -> e.plugins().updatePlugin(op == ToolboxCommando.Op.UPSERT, toDomTrip(a)));
                    case MANAGED_DEPENDENCIES ->
                        a -> (e -> e.dependencies()
                                .updateManagedDependency(op == ToolboxCommando.Op.UPSERT, toDomTrip(a)));
                    case DEPENDENCIES ->
                        a -> (e -> e.dependencies().updateDependency(op == ToolboxCommando.Op.UPSERT, toDomTrip(a)));
                    case EXTENSIONS ->
                        a -> (e -> e.extensions().updateExtension(op == ToolboxCommando.Op.UPSERT, toDomTrip(a)));
                };
            case DELETE ->
                switch (subject) {
                    case MANAGED_PLUGINS ->
                        profileId != null
                                ? a -> (e -> e.plugins().forProfile(profileId).deleteManagedPlugin(toDomTrip(a)))
                                : a -> (e -> e.plugins().deleteManagedPlugin(toDomTrip(a)));
                    case PLUGINS ->
                        profileId != null
                                ? a -> (e -> e.plugins().forProfile(profileId).deletePlugin(toDomTrip(a)))
                                : a -> (e -> e.plugins().deletePlugin(toDomTrip(a)));
                    case MANAGED_DEPENDENCIES -> a -> (e -> e.dependencies().deleteManagedDependency(toDomTrip(a)));
                    case DEPENDENCIES -> a -> (e -> e.dependencies().deleteDependency(toDomTrip(a)));
                    case EXTENSIONS -> a -> (e -> e.extensions().deleteExtension(toDomTrip(a)));
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
            Consumer<PomEditor> transformation = this.transformation.apply(artifactMapper.apply(artifact));
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
        PomEditor editor = new PomEditor(document);
        try (FileUtils.CollocatedTempFile tempFile = FileUtils.newTempFile(pom, false)) {
            for (Consumer<PomEditor> transformation : applicableTransformations) {
                transformation.accept(editor);
            }
            Files.writeString(tempFile.getPath(), document.toXml());
            tempFile.move();
        }
    }
}
