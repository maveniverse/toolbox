/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal.jdom;

import java.nio.file.Path;
import java.util.function.Consumer;
import org.jdom2.Document;

/**
 * The transformation context.
 */
public interface JDomTransformationContext {
    /**
     * The document,
     */
    Document getDocument();

    interface JDomPomTransformationContext extends JDomTransformationContext {
        /**
         * Hook for post processor registration.
         */
        void registerPostTransformation(Consumer<JDomPomTransformationContext> transformation);

        Path pom();
    }

    interface JdomExtensionsTransformationContext extends JDomTransformationContext {
        /**
         * Hook for post processor registration.
         */
        void registerPostTransformation(Consumer<JdomExtensionsTransformationContext> transformation);

        Path extensions();
    }
}
