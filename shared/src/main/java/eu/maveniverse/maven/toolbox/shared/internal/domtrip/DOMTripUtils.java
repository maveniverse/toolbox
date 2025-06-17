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
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Predicate;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;
import org.maveniverse.domtrip.maven.MavenExtensionsElements;
import org.maveniverse.domtrip.maven.MavenPomElements;
import org.maveniverse.domtrip.maven.PomEditor;

/**
 * Utils.
 */
public final class DOMTripUtils {
    private DOMTripUtils() {}

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

    public static String optionalTextContentOfDescendant(Element element, String descendant, String defaultValue) {
        return textContent(element.descendant(descendant).orElse(null), defaultValue);
    }

    public static String requiredTextContentOfDescendant(Element element, String descendant) {
        return textContent(element.descendant(descendant).orElseThrow());
    }

    public static String toGA(Element element) {
        requireNonNull(element);
        return requiredTextContentOfDescendant(element, MavenExtensionsElements.Elements.GROUP_ID) + ":"
                + requiredTextContentOfDescendant(element, MavenExtensionsElements.Elements.ARTIFACT_ID);
    }

    public static String toGATC(Element element) {
        requireNonNull(element);
        String type = optionalTextContentOfDescendant(element, MavenExtensionsElements.Elements.TYPE, "jar");
        String classifier = optionalTextContentOfDescendant(element, MavenExtensionsElements.Elements.CLASSIFIER, null);
        if (classifier != null) {
            return toGA(element) + ":" + type + ":" + classifier;
        } else {
            return toGA(element) + ":" + type;
        }
    }

    public static Predicate<Element> predicateGA(Artifact artifact) {
        requireNonNull(artifact);
        return element -> Objects.equals(toGA(element), artifact.getGroupId() + ":" + artifact.getArtifactId());
    }

    public static Predicate<Element> predicateGATC(Artifact artifact) {
        requireNonNull(artifact);
        return element -> Objects.equals(toGATC(element), ArtifactIdUtils.toVersionlessId(artifact));
    }

    public static Artifact gavToJarArtifact(Element element) {
        requireNonNull(element);
        return new DefaultArtifact(
                requiredTextContentOfDescendant(element, MavenExtensionsElements.Elements.GROUP_ID),
                requiredTextContentOfDescendant(element, MavenExtensionsElements.Elements.ARTIFACT_ID),
                "jar",
                requiredTextContentOfDescendant(element, MavenExtensionsElements.Elements.VERSION));
    }

    public static Artifact fromPom(Path pom) {
        requireNonNull(pom);
        PomEditor pomEditor = new PomEditor(Document.of(pom));
        String groupId = optionalTextContentOfDescendant(pomEditor.root(), MavenPomElements.Elements.GROUP_ID, null);
        String artifactId = requiredTextContentOfDescendant(pomEditor.root(), MavenPomElements.Elements.ARTIFACT_ID);
        String version = optionalTextContentOfDescendant(pomEditor.root(), MavenPomElements.Elements.VERSION, null);
        if (groupId == null || version == null) {
            Element parent = pomEditor.findChildElement(pomEditor.root(), MavenPomElements.Elements.PARENT);
            if (parent != null) {
                if (groupId == null) {
                    groupId = requiredTextContentOfDescendant(parent, MavenPomElements.Elements.GROUP_ID);
                }
                if (version == null) {
                    version = requiredTextContentOfDescendant(parent, MavenPomElements.Elements.VERSION);
                }
            } else {
                throw new IllegalArgumentException("Malformed POM: no groupId found");
            }
        }
        return new DefaultArtifact(groupId, artifactId, "pom", version).setFile(pom.toFile());
    }
}
