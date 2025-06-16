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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.aether.artifact.Artifact;
import org.maveniverse.domtrip.maven.ExtensionsEditor;
import org.maveniverse.domtrip.maven.MavenExtensionsElements;

/**
 * Enhanced extensions editor.
 */
public class SmartExtensionsEditor {
    private final ExtensionsEditor editor;

    public SmartExtensionsEditor(ExtensionsEditor editor) {
        this.editor = requireNonNull(editor);
        requireNonNull(editor.document());
    }

    public ExtensionsEditor editor() {
        return editor;
    }

    public List<Artifact> listExtensions() {
        return editor.root()
                .descendants(MavenExtensionsElements.Elements.EXTENSION)
                .map(DOMTripUtils::toArtifact)
                .toList();
    }

    public boolean updateExtension(boolean upsert, Artifact artifact) {
        requireNonNull(artifact);
        List<Element> matched = editor.root()
                .descendants(MavenExtensionsElements.Elements.EXTENSION)
                .filter(predicateGA(artifact))
                .toList();
        if (matched.isEmpty()) {
            if (!upsert) {
                return false;
            } else {
                editor.addExtension(
                        editor.root(), artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
                return true;
            }
        } else {
            if (matched.size() == 1) {
                Element element = matched.get(0);
                editor.findChildElement(element, MavenExtensionsElements.Elements.GROUP_ID)
                        .textContent(artifact.getGroupId());
                editor.findChildElement(element, MavenExtensionsElements.Elements.ARTIFACT_ID)
                        .textContent(artifact.getArtifactId());
                editor.findChildElement(element, MavenExtensionsElements.Elements.VERSION)
                        .textContent(artifact.getVersion());
                return true;
            } else {
                // TODO: what here? This is error, so fail? Or just update first match and drop the rest?
                throw new IllegalArgumentException("More than one matched extensions found");
            }
        }
    }

    public boolean deleteExtension(Artifact artifact) {
        requireNonNull(artifact);
        AtomicInteger counter = new AtomicInteger(0);
        editor.root()
                .descendants(MavenExtensionsElements.Elements.EXTENSION)
                .filter(predicateGA(artifact))
                .peek(e -> counter.incrementAndGet())
                .forEach(editor::removeElement);
        return counter.get() != 0;
    }
}
