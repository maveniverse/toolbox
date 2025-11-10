/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal.domtrip;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.domtrip.Document;
import eu.maveniverse.domtrip.Element;
import eu.maveniverse.domtrip.maven.MavenPomElements;
import eu.maveniverse.domtrip.maven.PomEditor;
import java.nio.file.Path;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

/**
 * Utils.
 */
public final class DOMTripUtils {
    private DOMTripUtils() {}

    public static Artifact toResolver(eu.maveniverse.domtrip.maven.Artifact artifact) {
        return new DefaultArtifact(
                artifact.groupId(), artifact.artifactId(), artifact.classifier(), artifact.type(), artifact.version());
    }

    public static eu.maveniverse.domtrip.maven.Artifact toDomTrip(Artifact artifact) {
        return eu.maveniverse.domtrip.maven.Artifact.of(
                artifact.getGroupId(),
                artifact.getArtifactId(),
                artifact.getVersion(),
                artifact.getClassifier(),
                artifact.getExtension());
    }

    public static String textContent(Element element, String defaultValue) {
        if (element != null) {
            if (element.textContent() != null) {
                return element.textContent();
            }
        }
        return defaultValue;
    }

    public static String textContent(Element element) {
        return textContent(element, null);
    }

    /**
     * Gets text content of a optional descendant element of given element with given name.
     */
    public static String optionalTextContentOfChild(Element element, String descendant, String defaultValue) {
        return textContent(element.child(descendant).orElse(null), defaultValue);
    }

    /**
     * Gets text content of a mandatory descendant element of given element with given name.
     */
    public static String requiredTextContentOfChild(Element element, String descendant) {
        return textContent(element.child(descendant).orElseThrow());
    }

    /**
     * Creates POM {@link Artifact} out of (existing) POM XML file. In reality, it merely reads GAV from it, and creates
     * {@code G:A:pom:V} artifact. To get G and V it may go for parent as well.
     */
    public static Artifact fromPom(Path pom) {
        requireNonNull(pom);
        PomEditor pomEditor = new PomEditor(Document.of(pom));
        String groupId = optionalTextContentOfChild(pomEditor.root(), MavenPomElements.Elements.GROUP_ID, null);
        String artifactId = requiredTextContentOfChild(pomEditor.root(), MavenPomElements.Elements.ARTIFACT_ID);
        String version = optionalTextContentOfChild(pomEditor.root(), MavenPomElements.Elements.VERSION, null);
        if (groupId == null || version == null) {
            Element parent = pomEditor.findChildElement(pomEditor.root(), MavenPomElements.Elements.PARENT);
            if (parent != null) {
                if (groupId == null) {
                    groupId = requiredTextContentOfChild(parent, MavenPomElements.Elements.GROUP_ID);
                }
                if (version == null) {
                    version = requiredTextContentOfChild(parent, MavenPomElements.Elements.VERSION);
                }
            } else {
                throw new IllegalArgumentException("Malformed POM: no groupId found");
            }
        }
        return new DefaultArtifact(groupId, artifactId, "pom", version).setFile(pom.toFile());
    }
}
