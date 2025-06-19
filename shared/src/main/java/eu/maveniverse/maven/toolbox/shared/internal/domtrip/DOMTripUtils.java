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
     * Constructs GA string out of {@link Element}.
     */
    public static String toGA(Element element) {
        requireNonNull(element);
        return requiredTextContentOfChild(element, MavenExtensionsElements.Elements.GROUP_ID) + ":"
                + requiredTextContentOfChild(element, MavenExtensionsElements.Elements.ARTIFACT_ID);
    }

    /**
     * Constructs GA string out of {@link Element} specific for Maven Plugins (G if absent is "org.apache.maven.plugins").
     */
    public static String toPluginGA(Element element) {
        requireNonNull(element);
        return optionalTextContentOfChild(
                        element, MavenExtensionsElements.Elements.GROUP_ID, "org.apache.maven.plugins")
                + ":" + requiredTextContentOfChild(element, MavenExtensionsElements.Elements.ARTIFACT_ID);
    }

    /**
     * Constructs GATC string out of {@link Element}.
     */
    public static String toGATC(Element element) {
        requireNonNull(element);
        String type = optionalTextContentOfChild(element, MavenExtensionsElements.Elements.TYPE, "jar");
        String classifier = optionalTextContentOfChild(element, MavenExtensionsElements.Elements.CLASSIFIER, null);
        if (classifier != null) {
            return toGA(element) + ":" + type + ":" + classifier;
        } else {
            return toGA(element) + ":" + type;
        }
    }

    /**
     * Element matcher predicate for GA.
     */
    public static Predicate<Element> predicateGA(Artifact artifact) {
        requireNonNull(artifact);
        return element -> Objects.equals(toGA(element), artifact.getGroupId() + ":" + artifact.getArtifactId());
    }

    /**
     * Element matcher predicate for GA.
     */
    public static Predicate<Element> predicatePluginGA(Artifact artifact) {
        requireNonNull(artifact);
        return element -> Objects.equals(toPluginGA(element), artifact.getGroupId() + ":" + artifact.getArtifactId());
    }

    /**
     * Element matcher predicate for GATC.
     */
    public static Predicate<Element> predicateGATC(Artifact artifact) {
        requireNonNull(artifact);
        return element -> Objects.equals(toGATC(element), ArtifactIdUtils.toVersionlessId(artifact));
    }

    /**
     * Creates a JAR {@link Artifact} out of {@link Element}. This method is usable ONLY when all three elements
     * are mandatory to be present, like in {@code extensions.xml} or alike.
     */
    public static Artifact gavToArtifact(Element element, String extension) {
        requireNonNull(element);
        return new DefaultArtifact(
                requiredTextContentOfChild(element, MavenExtensionsElements.Elements.GROUP_ID),
                requiredTextContentOfChild(element, MavenExtensionsElements.Elements.ARTIFACT_ID),
                extension,
                requiredTextContentOfChild(element, MavenExtensionsElements.Elements.VERSION));
    }

    /**
     * Creates a JAR {@link Artifact} out of {@link Element}.
     */
    public static Artifact gavToJarArtifact(Element element) {
        return gavToArtifact(element, "jar");
    }

    /**
     * Creates a POM {@link Artifact} out of {@link Element}.
     */
    public static Artifact gavToPomArtifact(Element element) {
        return gavToArtifact(element, "pom");
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
