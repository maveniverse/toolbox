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
import java.util.Optional;

/**
 * Enhanced POM editor.
 */
public class SmartPomEditor extends PomEditor {

    public SmartPomEditor(Document document) {
        super(document);
    }

    /**
     * Searches for {@code project/build/plugins/plugin[]} matching GA and removes {@code version} element of it.
     */
    public boolean removePluginVersion(eu.maveniverse.domtrip.maven.Artifact artifact) {
        requireNonNull(artifact);
        Element build = findChildElement(root(), MavenPomElements.Elements.BUILD);
        if (build != null) {
            Element plugins = findChildElement(build, MavenPomElements.Elements.PLUGINS);
            if (plugins != null) {
                Optional<Element> plugin =
                        plugins.child(MavenPomElements.Elements.PLUGIN).filter(artifact.predicateGA()).stream()
                                .findFirst();
                if (plugin.isPresent()) {
                    Element pe = plugin.orElseThrow();
                    Element version = findChildElement(pe, MavenPomElements.Elements.VERSION);
                    if (version != null) {
                        removeElement(version);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Updates/insert build extension. It goes for {@code project/build/extensions/extension[]} matched by GA.
     * If present, but version element is a property, it goes for {@link #updateProperty(boolean, String, String)}.
     */
    public boolean updateExtension(boolean upsert, eu.maveniverse.domtrip.maven.Artifact artifact) {
        requireNonNull(artifact);
        Element build = findChildElement(root(), MavenPomElements.Elements.BUILD);
        if (build == null && upsert) {
            build = insertMavenElement(root(), MavenPomElements.Elements.BUILD);
        }
        if (build != null) {
            Element extensions = findChildElement(build, MavenPomElements.Elements.EXTENSIONS);
            if (extensions == null && upsert) {
                extensions = insertMavenElement(build, MavenPomElements.Elements.EXTENSIONS);
            }
            if (extensions != null) {
                Element extension = extensions
                        .children(MavenPomElements.Elements.EXTENSION)
                        .filter(artifact.predicateGA())
                        .findFirst()
                        .orElse(null);
                if (extension == null && upsert) {
                    extension = insertMavenElement(extensions, MavenPomElements.Elements.EXTENSION);
                    insertMavenElement(extension, MavenPomElements.Elements.GROUP_ID, artifact.groupId());
                    insertMavenElement(extension, MavenPomElements.Elements.ARTIFACT_ID, artifact.artifactId());
                    insertMavenElement(extension, MavenPomElements.Elements.VERSION, artifact.version());
                    return true;
                }
                if (extension != null) {
                    Optional<Element> version = extension.child(MavenPomElements.Elements.VERSION);
                    if (version.isPresent()) {
                        String versionValue = version.orElseThrow().textContent();
                        if (versionValue.startsWith("${") && versionValue.endsWith("}")) {
                            String propertyKey = versionValue.substring(2, versionValue.length() - 1);
                            return updateProperty(false, propertyKey, artifact.version());
                        } else {
                            version.orElseThrow().textContent(artifact.version());
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Removes build extension.  It goes for {@code project/build/extensions/extension[]} matched by GA and removes
     * entry if present.
     */
    public boolean deleteExtension(eu.maveniverse.domtrip.maven.Artifact artifact) {
        requireNonNull(artifact);
        Element build = findChildElement(root(), MavenPomElements.Elements.BUILD);
        if (build != null) {
            Element extensions = findChildElement(build, MavenPomElements.Elements.EXTENSIONS);
            if (extensions != null) {
                Element extension = extensions
                        .children(MavenPomElements.Elements.EXTENSION)
                        .filter(artifact.predicateGA())
                        .findFirst()
                        .orElse(null);
                if (extension != null) {
                    return removeElement(extension);
                }
            }
        }
        return false;
    }

    /**
     * Updates/insert parent. It goes for {@code project/parent} and rewrites it according to passed in coordinates.
     */
    public boolean updateParent(boolean upsert, eu.maveniverse.domtrip.maven.Artifact artifact) {
        requireNonNull(artifact);
        Element parent = findChildElement(root(), MavenPomElements.Elements.PARENT);
        if (parent == null && upsert) {
            parent = insertMavenElement(root(), MavenPomElements.Elements.PARENT);
        }
        if (parent != null) {
            insertMavenElement(parent, MavenPomElements.Elements.GROUP_ID, artifact.groupId());
            insertMavenElement(parent, MavenPomElements.Elements.ARTIFACT_ID, artifact.artifactId());
            insertMavenElement(parent, MavenPomElements.Elements.VERSION, artifact.version());
            return true;
        }
        return false;
    }

    /**
     * Removes parent.  It goes for {@code project/parent} and removes entry if present.
     */
    public boolean deleteParent() {
        Element parent = findChildElement(root(), MavenPomElements.Elements.PARENT);
        if (parent != null) {
            removeElement(parent);
            return true;
        }
        return false;
    }
}
