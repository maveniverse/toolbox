/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal.domtrip;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.domtrip.Element;
import java.util.Objects;
import java.util.function.Predicate;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.maveniverse.domtrip.maven.MavenExtensionsElements;

/**
 * Utils.
 */
public final class DOMTripUtils {
    private DOMTripUtils() {}

    public static String textContent(Element element) {
        if (element != null) {
            if (element.textContent() != null) {
                return element.textContent();
            }
        }
        return null;
    }

    public static Predicate<Element> predicateGA(Artifact artifact) {
        requireNonNull(artifact);
        return element -> Objects.equals(
                        artifact.getGroupId(),
                        textContent(element.descendant(MavenExtensionsElements.Elements.GROUP_ID)
                                .orElse(null)))
                && Objects.equals(
                        artifact.getArtifactId(),
                        textContent(element.descendant(MavenExtensionsElements.Elements.ARTIFACT_ID)
                                .orElse(null)));
    }

    public static Predicate<Element> predicateGATC(Artifact artifact) {
        requireNonNull(artifact);
        return element -> {
            String groupId = textContent(element.descendant(MavenExtensionsElements.Elements.GROUP_ID)
                    .orElse(null));
            String artifactId = textContent(element.descendant(MavenExtensionsElements.Elements.ARTIFACT_ID)
                    .orElse(null));
            String type = textContent(
                    element.descendant(MavenExtensionsElements.Elements.TYPE).orElse(null));
            if (type == null) {
                type = "jar";
            }
            String classifier = textContent(element.descendant(MavenExtensionsElements.Elements.CLASSIFIER)
                    .orElse(null));
            if (classifier == null) {
                classifier = "";
            }
            return Objects.equals(artifact.getGroupId(), groupId)
                    && Objects.equals(artifact.getArtifactId(), artifactId)
                    && Objects.equals(artifact.getClassifier(), classifier)
                    && Objects.equals(artifact.getExtension(), type);
        };
    }

    public static Artifact toArtifact(Element element) {
        requireNonNull(element);
        return new DefaultArtifact(
                element.descendant(MavenExtensionsElements.Elements.GROUP_ID)
                        .orElseThrow()
                        .textContent(),
                element.descendant(MavenExtensionsElements.Elements.GROUP_ID)
                        .orElseThrow()
                        .textContent(),
                "jar",
                element.descendant(MavenExtensionsElements.Elements.GROUP_ID)
                        .orElseThrow()
                        .textContent());
    }
}
