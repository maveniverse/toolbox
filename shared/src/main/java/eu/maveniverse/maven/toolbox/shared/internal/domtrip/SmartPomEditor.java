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
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.aether.artifact.Artifact;
import org.maveniverse.domtrip.maven.MavenPomElements;
import org.maveniverse.domtrip.maven.PomEditor;

/**
 * Enhanced POM editor.
 */
public class SmartPomEditor {
    private final PomEditor editor;

    public SmartPomEditor(PomEditor editor) {
        this.editor = requireNonNull(editor);
        requireNonNull(editor.document());
    }

    public PomEditor editor() {
        return editor;
    }

    public boolean updateProperty(boolean upsert, String key, String value) {
        requireNonNull(key);
        requireNonNull(value);
        Element properties =
                editor.root().descendant(MavenPomElements.Elements.PROPERTIES).orElse(null);
        if (properties == null && upsert) {
            properties = editor.insertMavenElement(editor.root(), MavenPomElements.Elements.PROPERTIES);
        }
        if (properties != null) {
            Element property = properties.descendant(key).orElse(null);
            if (property == null && upsert) {
                property = editor.insertMavenElement(properties, key);
            }
            if (property != null) {
                property.textContent(value);
                return true;
            }
        }
        return false;
    }

    public boolean deleteProperty(String key, String value) {
        requireNonNull(key);
        requireNonNull(value);
        Element properties =
                editor.root().descendant(MavenPomElements.Elements.PROPERTIES).orElse(null);
        if (properties != null) {
            Element property = properties.descendant(key).orElse(null);
            if (property != null) {
                editor.removeElement(property);
                return true;
            }
        }
        return false;
    }

    public void setParent(Artifact artifact) {
        requireNonNull(artifact);
        Element parent = editor.findChildElement(editor.root(), MavenPomElements.Elements.PARENT);
        if (parent == null) {
            parent = editor.insertMavenElement(editor.root(), MavenPomElements.Elements.PARENT);
        }
        editor.insertMavenElement(parent, MavenPomElements.Elements.GROUP_ID).textContent(artifact.getGroupId());
        editor.insertMavenElement(parent, MavenPomElements.Elements.ARTIFACT_ID).textContent(artifact.getArtifactId());
        editor.insertMavenElement(parent, MavenPomElements.Elements.VERSION).textContent(artifact.getVersion());
    }

    public void setVersion(String ver) {
        requireNonNull(ver);
        Element version = editor.findChildElement(editor.root(), MavenPomElements.Elements.VERSION);
        if (version == null) {
            Element parent = editor.findChildElement(editor.root(), MavenPomElements.Elements.PARENT);
            if (parent != null) {
                version = editor.findChildElement(parent, MavenPomElements.Elements.VERSION);
            }
        }
        if (version != null) {
            version.textContent(ver);
            return;
        }
        throw new IllegalArgumentException("Could not set version");
    }

    public void setPackaging(String pkg) {
        requireNonNull(pkg);
        Element packaging = editor.findChildElement(editor.root(), MavenPomElements.Elements.PACKAGING);
        if (packaging == null) {
            if ("jar".equals(pkg)) {
                return;
            }
            packaging = editor.insertMavenElement(editor.root(), MavenPomElements.Elements.PACKAGING);
        }
        packaging.textContent(pkg);
    }

    public boolean addSubProject(String subProject) {
        requireNonNull(subProject);
        Element modules = editor.findChildElement(editor.root(), MavenPomElements.Elements.MODULES);
        if (modules == null) {
            modules = editor.insertMavenElement(editor.root(), MavenPomElements.Elements.MODULES);
        }
        List<String> existing = modules.descendants(MavenPomElements.Elements.MODULE)
                .map(Element::textContent)
                .toList();
        if (!existing.contains(subProject)) {
            Element module = editor.insertMavenElement(modules, MavenPomElements.Elements.MODULE);
            module.textContent(subProject);
            return true;
        }
        return false;
    }

    public boolean removeSubProject(String subProject) {
        requireNonNull(subProject);
        Element modules = editor.findChildElement(editor.root(), MavenPomElements.Elements.MODULES);
        if (modules == null) {
            return false;
        }
        AtomicBoolean removed = new AtomicBoolean(false);
        modules.descendants(MavenPomElements.Elements.MODULE)
                .filter(e -> Objects.equals(subProject, e.textContent()))
                .peek(e -> removed.set(true))
                .forEach(editor::removeElement);
        return removed.get();
    }

    public boolean removePluginVersion(Artifact artifact) {
        requireNonNull(artifact);
        Element build = editor.findChildElement(editor.root(), MavenPomElements.Elements.BUILD);
        if (build != null) {
            Element plugins = editor.findChildElement(build, MavenPomElements.Elements.PLUGINS);
            if (plugins != null) {
                Optional<Element> plugin =
                        plugins.descendant(MavenPomElements.Elements.PLUGIN).filter(predicateGA(artifact)).stream()
                                .findFirst();
                if (plugin.isPresent()) {
                    Element pe = plugin.orElseThrow();
                    Element version = editor.findChildElement(pe, MavenPomElements.Elements.VERSION);
                    if (version != null) {
                        editor.removeElement(version);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean updateManagedDependency(boolean upsert, Artifact artifact) {
        requireNonNull(artifact);
    }

    public boolean deleteManagedDependency(Artifact artifact) {
        requireNonNull(artifact);
    }

    public boolean updateDependency(boolean upsert, Artifact artifact) {
        requireNonNull(artifact);
    }

    public boolean deleteDependency(Artifact artifact) {
        requireNonNull(artifact);
    }

    public boolean updateManagedPlugin(boolean upsert, Artifact artifact) {
        requireNonNull(artifact);
    }

    public boolean deleteManagedPlugin(Artifact artifact) {
        requireNonNull(artifact);
    }

    public boolean updatePlugin(boolean upsert, Artifact artifact) {
        requireNonNull(artifact);
    }

    public boolean deletePlugin(Artifact artifact) {
        requireNonNull(artifact);
    }

    public boolean updateExtension(boolean upsert, Artifact artifact) {
        requireNonNull(artifact);
    }

    public boolean deleteExtension(Artifact artifact) {
        requireNonNull(artifact);
    }
}
