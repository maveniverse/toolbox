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
 * Construction to accept collection of artifacts, and applies it to some extensions.xml based on provided transformations.
 */
public final class JDomExtensionsTransformer {

    public static Function<Artifact, Consumer<JDomTransformationContext.JdomExtensionsTransformationContext>>
            updateExtension(boolean upsert) {
        return a -> context ->
                JDomExtensionsEditor.updateExtension(context.getDocument().getRootElement(), a, upsert);
    }

    public static Function<Artifact, Consumer<JDomTransformationContext.JdomExtensionsTransformationContext>>
            deleteExtension() {
        return a -> context -> {
            JDomExtensionsEditor.deleteExtension(context.getDocument().getRootElement(), a);
        };
    }

    private final Path extensions;

    public JDomExtensionsTransformer(Path extensions) {
        this.extensions = requireNonNull(extensions);
    }

    public void apply(List<Consumer<JDomTransformationContext.JdomExtensionsTransformationContext>> transformations)
            throws IOException {
        requireNonNull(transformations, "transformations");
        if (!transformations.isEmpty()) {
            try (JDomDocumentIO domDocumentIO = new JDomDocumentIO(extensions)) {
                ArrayList<Consumer<JDomTransformationContext.JdomExtensionsTransformationContext>> postProcessors =
                        new ArrayList<>();
                JDomTransformationContext.JdomExtensionsTransformationContext context =
                        new JDomTransformationContext.JdomExtensionsTransformationContext() {
                            @Override
                            public Document getDocument() {
                                return domDocumentIO.getDocument();
                            }

                            @Override
                            public void registerPostTransformation(
                                    Consumer<JdomExtensionsTransformationContext> transformation) {
                                postProcessors.add(transformation);
                            }

                            @Override
                            public Path extensions() {
                                return extensions;
                            }
                        };
                for (Consumer<JDomTransformationContext.JdomExtensionsTransformationContext> transformation :
                        transformations) {
                    transformation.accept(context);
                }
                for (Consumer<JDomTransformationContext.JdomExtensionsTransformationContext> transformation :
                        postProcessors) {
                    transformation.accept(context);
                }
            }
        }
    }
}
