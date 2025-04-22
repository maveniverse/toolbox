/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal.jdom;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import org.eclipse.aether.artifact.Artifact;
import org.jdom2.Document;

/**
 * Construction to accept collection of artifacts, and applies it to some POM based on provided transformations.
 */
public final class JDomPomTransformer {
    /**
     * The transformation context.
     */
    public interface TransformationContext {
        boolean pomHasParent();

        Document getDocument();

        void registerPostTransformation(Consumer<TransformationContext> transformation);
    }

    /**
     * Removes empty remnant tags, like {@code <plugins />}.
     */
    private static final Consumer<TransformationContext> removeEmptyElements = ctx -> {
        JDomCleanupHelper.cleanup(ctx.getDocument().getRootElement());
    };

    private static Consumer<TransformationContext> setProperty(String key, String value, boolean upsert) {
        return context -> JDomPomEditor.setProperty(context.getDocument().getRootElement(), key, value, upsert);
    }

    public static Function<Artifact, Consumer<TransformationContext>> updateManagedPlugin(boolean upsert) {
        return a -> context ->
                JDomPomEditor.updateManagedPlugin(context.getDocument().getRootElement(), a, upsert);
    }

    public static Function<Artifact, Consumer<TransformationContext>> deleteManagedPlugin() {
        return a -> context -> {
            JDomPomEditor.deleteManagedPlugin(context.getDocument().getRootElement(), a);
            context.registerPostTransformation(removeEmptyElements);
        };
    }

    public static Function<Artifact, Consumer<TransformationContext>> updatePlugin(boolean upsert) {
        return a -> context -> JDomPomEditor.updatePlugin(context.getDocument().getRootElement(), a, upsert);
    }

    public static Function<Artifact, Consumer<TransformationContext>> deletePlugin() {
        return a -> context -> {
            JDomPomEditor.deletePlugin(context.getDocument().getRootElement(), a);
            context.registerPostTransformation(removeEmptyElements);
        };
    }

    public static Function<Artifact, Consumer<TransformationContext>> updateManagedDependency(boolean upsert) {
        return a -> context ->
                JDomPomEditor.updateManagedDependency(context.getDocument().getRootElement(), a, upsert);
    }

    public static Function<Artifact, Consumer<TransformationContext>> deleteManagedDependency() {
        return a -> context -> {
            JDomPomEditor.deleteManagedDependency(context.getDocument().getRootElement(), a);
            context.registerPostTransformation(removeEmptyElements);
        };
    }

    public static Function<Artifact, Consumer<TransformationContext>> updateDependency(boolean upsert) {
        return a ->
                context -> JDomPomEditor.updateDependency(context.getDocument().getRootElement(), a, upsert);
    }

    public static Function<Artifact, Consumer<TransformationContext>> deleteDependency() {
        return a -> context -> {
            JDomPomEditor.deleteDependency(context.getDocument().getRootElement(), a);
            context.registerPostTransformation(removeEmptyElements);
        };
    }

    public void apply(Path pom, List<Consumer<TransformationContext>> transformations) throws IOException {
        requireNonNull(pom, "pom");
        requireNonNull(transformations, "transformations");
        if (!transformations.isEmpty()) {
            try (JDomDocumentIO domDocumentIO = new JDomDocumentIO(pom)) {
                ArrayList<Consumer<TransformationContext>> postProcessors = new ArrayList<>();
                TransformationContext context = new TransformationContext() {
                    @Override
                    public boolean pomHasParent() {
                        return domDocumentIO
                                        .getDocument()
                                        .getRootElement()
                                        .getChild(
                                                "parent",
                                                domDocumentIO
                                                        .getDocument()
                                                        .getRootElement()
                                                        .getNamespace())
                                != null;
                    }

                    @Override
                    public Document getDocument() {
                        return domDocumentIO.getDocument();
                    }

                    @Override
                    public void registerPostTransformation(Consumer<TransformationContext> transformation) {
                        postProcessors.add(transformation);
                    }
                };
                for (Consumer<TransformationContext> transformation : transformations) {
                    transformation.accept(context);
                }
                for (Consumer<TransformationContext> transformation : postProcessors) {
                    transformation.accept(context);
                }
            }
        }
    }
}
