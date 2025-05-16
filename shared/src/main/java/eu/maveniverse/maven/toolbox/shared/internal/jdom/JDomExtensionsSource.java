/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal.jdom;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.toolbox.shared.internal.Artifacts;
import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.eclipse.aether.artifact.Artifact;

/**
 * Construction to accept collection of artifacts, and applies it to some extensions.xml based on provided transformations.
 */
public final class JDomExtensionsSource implements Artifacts.Source {

    private final Path extensions;

    public JDomExtensionsSource(Path extensions) {
        this.extensions = requireNonNull(extensions);
    }

    @Override
    public Stream<Artifact> get() throws IOException {
        try (JDomDocumentIO domDocumentIO = new JDomDocumentIO(extensions)) {
            return JDomExtensionsEditor.listExtensions(
                    domDocumentIO.getDocument().getRootElement())
                    .stream();
        }
    }
}
