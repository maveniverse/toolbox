/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal.domtrip;

import static eu.maveniverse.maven.toolbox.shared.internal.domtrip.DOMTripUtils.predicateGA;
import static java.util.Objects.requireNonNull;

import eu.maveniverse.domtrip.Element;
import eu.maveniverse.domtrip.maven.ExtensionsEditor;
import eu.maveniverse.domtrip.maven.MavenExtensionsElements;
import eu.maveniverse.maven.shared.core.component.ComponentSupport;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.aether.artifact.Artifact;

/**
 * Enhanced extensions editor.
 */
public class SmartExtensionsEditor extends ComponentSupport {
    private final ExtensionsEditor editor;

    public SmartExtensionsEditor(ExtensionsEditor editor) {
        this.editor = requireNonNull(editor);
        requireNonNull(editor.document());
    }

    public ExtensionsEditor editor() {
        return editor;
    }

    /**
     * Lists enlisted extensions as {@link Artifact} (they are always JARs without classifiers).
     */
    public List<Artifact> listExtensions() {
        return editor.root()
                .children(MavenExtensionsElements.Elements.EXTENSION)
                .map(DOMTripUtils::gavToJarArtifact)
                .toList();
    }

    /**
     * Updates (existing) or inserts (if upsert and not enlisted) an extension. Existence is checked by GA matching
     * only (as extensions are JARs and have no classifiers).
     */
    public boolean updateExtension(boolean upsert, Artifact artifact) {
        requireNonNull(artifact);
        List<Element> matched = editor.root()
                .children(MavenExtensionsElements.Elements.EXTENSION)
                .filter(predicateGA(artifact))
                .toList();
        if (matched.isEmpty()) {
            if (upsert) {
                editor.addExtension(
                        editor.root(), artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
                return true;
            } else {
                return false;
            }
        } else {
            Element element = matched.get(0);
            editor.findChildElement(element, MavenExtensionsElements.Elements.VERSION)
                    .textContent(artifact.getVersion());
            if (matched.size() > 1) {
                logger.warn("More than one matching extension found: {}", matched.size());
            }
            return true;
        }
    }

    /**
     * Removes an extension. Existence is checked by GA matching only (as extensions are JARs and have no classifiers).
     */
    public boolean deleteExtension(Artifact artifact) {
        requireNonNull(artifact);
        AtomicInteger counter = new AtomicInteger(0);
        editor.root()
                .children(MavenExtensionsElements.Elements.EXTENSION)
                .filter(predicateGA(artifact))
                .peek(e -> counter.incrementAndGet())
                .forEach(editor::removeElement);
        return counter.get() != 0;
    }
}
