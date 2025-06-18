/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal.domtrip;

import static eu.maveniverse.maven.toolbox.shared.internal.domtrip.DOMTripUtils.predicateGA;
import static eu.maveniverse.maven.toolbox.shared.internal.domtrip.DOMTripUtils.predicateGATC;
import static eu.maveniverse.maven.toolbox.shared.internal.domtrip.DOMTripUtils.predicatePluginGA;
import static java.util.Objects.requireNonNull;

import eu.maveniverse.domtrip.Element;
import eu.maveniverse.maven.shared.core.component.ComponentSupport;
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
public class SmartPomEditor extends ComponentSupport {
    private final PomEditor editor;

    public SmartPomEditor(PomEditor editor) {
        this.editor = requireNonNull(editor);
        requireNonNull(editor.document());
    }

    public PomEditor editor() {
        return editor;
    }

    /**
     * Updates/inserts value of {@code project/properties/key}.
     */
    public boolean updateProperty(boolean upsert, String key, String value) {
        requireNonNull(key);
        requireNonNull(value);
        Element properties =
                editor.root().child(MavenPomElements.Elements.PROPERTIES).orElse(null);
        if (properties == null && upsert) {
            properties = editor.insertMavenElement(editor.root(), MavenPomElements.Elements.PROPERTIES);
        }
        if (properties != null) {
            Element property = properties.child(key).orElse(null);
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

    /**
     * Removes {@code project/properties/key}.
     */
    public boolean deleteProperty(String key) {
        requireNonNull(key);
        Element properties =
                editor.root().child(MavenPomElements.Elements.PROPERTIES).orElse(null);
        if (properties != null) {
            Element property = properties.child(key).orElse(null);
            if (property != null) {
                editor.removeElement(property);
                return true;
            }
        }
        return false;
    }

    /**
     * Sets {@code project/parent} to given GAV.
     */
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

    /**
     * Sets {@code project/version} or {@code project/parent/version} (if previous not exists) to given value.
     */
    public void setVersion(String value) {
        requireNonNull(value);
        Element version = editor.findChildElement(editor.root(), MavenPomElements.Elements.VERSION);
        if (version == null) {
            Element parent = editor.findChildElement(editor.root(), MavenPomElements.Elements.PARENT);
            if (parent != null) {
                version = editor.findChildElement(parent, MavenPomElements.Elements.VERSION);
            }
        }
        if (version != null) {
            version.textContent(value);
            return;
        }
        throw new IllegalArgumentException("Could not set version");
    }

    /**
     * Sets {@code project/packaging} to given value.
     */
    public void setPackaging(String value) {
        requireNonNull(value);
        Element packaging = editor.findChildElement(editor.root(), MavenPomElements.Elements.PACKAGING);
        if (packaging == null) {
            editor.insertMavenElement(editor.root(), MavenPomElements.Elements.PACKAGING, value);
        } else {
            packaging.textContent(value);
        }
    }

    /**
     * Adds a {@code project/modules/module[]} entry with given value, if not present.
     */
    public boolean addSubProject(String value) {
        requireNonNull(value);
        Element modules = editor.findChildElement(editor.root(), MavenPomElements.Elements.MODULES);
        if (modules == null) {
            modules = editor.insertMavenElement(editor.root(), MavenPomElements.Elements.MODULES);
        }
        List<String> existing = modules.children(MavenPomElements.Elements.MODULE)
                .map(Element::textContent)
                .toList();
        if (!existing.contains(value)) {
            Element module = editor.insertMavenElement(modules, MavenPomElements.Elements.MODULE);
            module.textContent(value);
            return true;
        }
        return false;
    }

    /**
     * Removes a {@code project/modules/module[]} entry with given value, if present.
     */
    public boolean removeSubProject(String value) {
        requireNonNull(value);
        Element modules = editor.findChildElement(editor.root(), MavenPomElements.Elements.MODULES);
        if (modules == null) {
            return false;
        }
        AtomicBoolean removed = new AtomicBoolean(false);
        modules.children(MavenPomElements.Elements.MODULE)
                .filter(e -> Objects.equals(value, e.textContent()))
                .peek(e -> removed.set(true))
                .forEach(editor::removeElement);
        return removed.get();
    }

    /**
     * Searches for {@code project/build/plugins/plugin[]} matching GA and removes {@code version} element of it.
     */
    public boolean removePluginVersion(Artifact artifact) {
        requireNonNull(artifact);
        Element build = editor.findChildElement(editor.root(), MavenPomElements.Elements.BUILD);
        if (build != null) {
            Element plugins = editor.findChildElement(build, MavenPomElements.Elements.PLUGINS);
            if (plugins != null) {
                Optional<Element> plugin =
                        plugins.child(MavenPomElements.Elements.PLUGIN).filter(predicateGA(artifact)).stream()
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

    /**
     * Updates/inserts managed dependency. It goes for {@code project/dependencyManagement/dependencies/dependency[]}
     * matched by GATC. If entry found but version is a property, it goes for {@link #updateProperty(boolean, String, String)}.
     */
    public boolean updateManagedDependency(boolean upsert, Artifact artifact) {
        requireNonNull(artifact);
        Element dependencyManagement =
                editor.findChildElement(editor.root(), MavenPomElements.Elements.DEPENDENCY_MANAGEMENT);
        if (dependencyManagement == null && upsert) {
            dependencyManagement =
                    editor.insertMavenElement(editor.root(), MavenPomElements.Elements.DEPENDENCY_MANAGEMENT);
        }
        if (dependencyManagement != null) {
            Element dependencies =
                    editor.findChildElement(dependencyManagement, MavenPomElements.Elements.DEPENDENCIES);
            if (dependencies == null && upsert) {
                dependencies = editor.insertMavenElement(dependencyManagement, MavenPomElements.Elements.DEPENDENCIES);
            }
            if (dependencies != null) {
                Element dependency = dependencies
                        .children(MavenPomElements.Elements.DEPENDENCY)
                        .filter(predicateGATC(artifact))
                        .findFirst()
                        .orElse(null);
                if (dependency == null && upsert) {
                    editor.addDependency(
                            dependencies, artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
                    if (!"jar".equals(artifact.getExtension())) {
                        editor.insertMavenElement(dependency, MavenPomElements.Elements.TYPE, artifact.getExtension());
                    }
                    if (!artifact.getClassifier().trim().isEmpty()) {
                        editor.insertMavenElement(
                                dependency, MavenPomElements.Elements.CLASSIFIER, artifact.getClassifier());
                    }
                    return true;
                }
                if (dependency != null) {
                    Optional<Element> version = dependency.child(MavenPomElements.Elements.VERSION);
                    if (version.isPresent()) {
                        String versionValue = version.orElseThrow().textContent();
                        if (versionValue.startsWith("${") && versionValue.endsWith("}")) {
                            String propertyKey = versionValue.substring(2, versionValue.length() - 1);
                            return updateProperty(false, propertyKey, artifact.getVersion());
                        } else {
                            version.orElseThrow().textContent(artifact.getVersion());
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Removes managed dependency. It goes for {@code project/dependencyManagement/dependencies/dependency[]}
     * matched by GATC and removes it if present.
     */
    public boolean deleteManagedDependency(Artifact artifact) {
        requireNonNull(artifact);
        Element dependencyManagement =
                editor.findChildElement(editor.root(), MavenPomElements.Elements.DEPENDENCY_MANAGEMENT);
        if (dependencyManagement != null) {
            Element dependencies =
                    editor.findChildElement(dependencyManagement, MavenPomElements.Elements.DEPENDENCIES);
            if (dependencies != null) {
                Element dependency = dependencies
                        .children(MavenPomElements.Elements.DEPENDENCY)
                        .filter(predicateGATC(artifact))
                        .findFirst()
                        .orElse(null);
                if (dependency != null) {
                    return editor.removeElement(dependency);
                }
            }
        }
        return false;
    }

    /**
     * Updates/inserts dependency. It goes for {@code project/dependencies/dependency[]}
     * matched by GATC. If entry found but version is a property, it goes for {@link #updateProperty(boolean, String, String)}.
     * If entry found but has no version element, it goes for {@link #updateManagedDependency(boolean, Artifact)}.
     */
    public boolean updateDependency(boolean upsert, Artifact artifact) {
        requireNonNull(artifact);
        Element dependencies = editor.findChildElement(editor.root(), MavenPomElements.Elements.DEPENDENCIES);
        if (dependencies == null && upsert) {
            dependencies = editor.insertMavenElement(editor.root(), MavenPomElements.Elements.DEPENDENCIES);
        }
        if (dependencies != null) {
            Element dependency = dependencies
                    .children(MavenPomElements.Elements.DEPENDENCY)
                    .filter(predicateGATC(artifact))
                    .findFirst()
                    .orElse(null);
            if (dependency == null && upsert) {
                editor.addDependency(
                        dependencies, artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
                if (!"jar".equals(artifact.getExtension())) {
                    editor.insertMavenElement(dependency, MavenPomElements.Elements.TYPE, artifact.getExtension());
                }
                if (!artifact.getClassifier().trim().isEmpty()) {
                    editor.insertMavenElement(
                            dependency, MavenPomElements.Elements.CLASSIFIER, artifact.getClassifier());
                }
                return true;
            }
            if (dependency != null) {
                Optional<Element> version = dependency.child(MavenPomElements.Elements.VERSION);
                if (version.isPresent()) {
                    String versionValue = version.orElseThrow().textContent();
                    if (versionValue.startsWith("${") && versionValue.endsWith("}")) {
                        String propertyKey = versionValue.substring(2, versionValue.length() - 1);
                        return updateProperty(false, propertyKey, artifact.getVersion());
                    } else {
                        version.orElseThrow().textContent(artifact.getVersion());
                        return true;
                    }
                } else {
                    return updateManagedDependency(false, artifact);
                }
            }
        }
        return false;
    }

    /**
     * Removes dependency. It goes for {@code project/dependencies/dependency[]}
     * matched by GATC and removes it if present.
     */
    public boolean deleteDependency(Artifact artifact) {
        requireNonNull(artifact);
        Element dependencies = editor.findChildElement(editor.root(), MavenPomElements.Elements.DEPENDENCIES);
        if (dependencies != null) {
            Element dependency = dependencies
                    .children(MavenPomElements.Elements.DEPENDENCY)
                    .filter(predicateGATC(artifact))
                    .findFirst()
                    .orElse(null);
            if (dependency != null) {
                return editor.removeElement(dependency);
            }
        }
        return false;
    }

    /**
     * Updates/inserts managed plugin. It goes for {@code project/build/pluginManagement/plugins/plugin[]} matched
     * by GA. If entry found, but version is a property, it goes for {@link #updateProperty(boolean, String, String)}.
     */
    public boolean updateManagedPlugin(boolean upsert, Artifact artifact) {
        requireNonNull(artifact);
        Element build = editor.findChildElement(editor.root(), MavenPomElements.Elements.BUILD);
        if (build == null && upsert) {
            build = editor.insertMavenElement(editor.root(), MavenPomElements.Elements.BUILD);
        }
        if (build != null) {
            Element pluginManagement = editor.findChildElement(build, MavenPomElements.Elements.PLUGIN_MANAGEMENT);
            if (pluginManagement == null && upsert) {
                pluginManagement = editor.insertMavenElement(build, MavenPomElements.Elements.PLUGIN_MANAGEMENT);
            }
            if (pluginManagement != null) {
                Element plugins = editor.findChildElement(pluginManagement, MavenPomElements.Elements.PLUGINS);
                if (plugins == null && upsert) {
                    plugins = editor.insertMavenElement(pluginManagement, MavenPomElements.Elements.PLUGINS);
                }
                if (plugins != null) {
                    Element plugin = plugins.children(MavenPomElements.Elements.PLUGIN)
                            .filter(predicatePluginGA(artifact))
                            .findFirst()
                            .orElse(null);
                    if (plugin == null && upsert) {
                        plugin = editor.insertMavenElement(plugins, MavenPomElements.Elements.PLUGIN);
                        editor.insertMavenElement(plugin, MavenPomElements.Elements.GROUP_ID, artifact.getGroupId());
                        editor.insertMavenElement(
                                plugin, MavenPomElements.Elements.ARTIFACT_ID, artifact.getArtifactId());
                        editor.insertMavenElement(plugin, MavenPomElements.Elements.VERSION, artifact.getVersion());
                        return true;
                    }
                    if (plugin != null) {
                        Optional<Element> version = plugin.child(MavenPomElements.Elements.VERSION);
                        if (version.isPresent()) {
                            String versionValue = version.orElseThrow().textContent();
                            if (versionValue.startsWith("${") && versionValue.endsWith("}")) {
                                String propertyKey = versionValue.substring(2, versionValue.length() - 1);
                                return updateProperty(false, propertyKey, artifact.getVersion());
                            } else {
                                version.orElseThrow().textContent(artifact.getVersion());
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Removes managed plugin. It goes for {@code project/build/pluginManagement/plugins[]}  matched by GA and
     * removes it if present.
     */
    public boolean deleteManagedPlugin(Artifact artifact) {
        requireNonNull(artifact);
        Element build = editor.findChildElement(editor.root(), MavenPomElements.Elements.BUILD);
        if (build != null) {
            Element pluginManagement = editor.findChildElement(build, MavenPomElements.Elements.PLUGIN_MANAGEMENT);
            if (pluginManagement != null) {
                Element plugins = editor.findChildElement(pluginManagement, MavenPomElements.Elements.PLUGINS);
                if (plugins != null) {
                    Element plugin = plugins.children(MavenPomElements.Elements.PLUGIN)
                            .filter(predicatePluginGA(artifact))
                            .findFirst()
                            .orElse(null);
                    if (plugin != null) {
                        return editor.removeElement(plugin);
                    }
                }
            }
        }
        return false;
    }

    /**
     * Updates/inserts plugin. It goes for {@code project/build/plugins/plugin[]} matched by GA. If entry found, but
     * version is a property, it goes for {@link #updateProperty(boolean, String, String)}. If found but no
     * version element present, it goes for {@link #updateManagedPlugin(boolean, Artifact)}.
     */
    public boolean updatePlugin(boolean upsert, Artifact artifact) {
        requireNonNull(artifact);
        Element build = editor.findChildElement(editor.root(), MavenPomElements.Elements.BUILD);
        if (build == null && upsert) {
            build = editor.insertMavenElement(editor.root(), MavenPomElements.Elements.BUILD);
        }
        if (build != null) {
            Element plugins = editor.findChildElement(build, MavenPomElements.Elements.PLUGINS);
            if (plugins == null && upsert) {
                plugins = editor.insertMavenElement(build, MavenPomElements.Elements.PLUGINS);
            }
            if (plugins != null) {
                Element plugin = plugins.children(MavenPomElements.Elements.PLUGIN)
                        .filter(predicatePluginGA(artifact))
                        .findFirst()
                        .orElse(null);
                if (plugin == null && upsert) {
                    editor.addPlugin(plugins, artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
                    return true;
                }
                if (plugin != null) {
                    Optional<Element> version = plugin.child(MavenPomElements.Elements.VERSION);
                    if (version.isPresent()) {
                        String versionValue = version.orElseThrow().textContent();
                        if (versionValue.startsWith("${") && versionValue.endsWith("}")) {
                            String propertyKey = versionValue.substring(2, versionValue.length() - 1);
                            return updateProperty(false, propertyKey, artifact.getVersion());
                        } else {
                            version.orElseThrow().textContent(artifact.getVersion());
                            return true;
                        }
                    } else {
                        return updateManagedPlugin(upsert, artifact);
                    }
                }
            }
        }
        return false;
    }

    /**
     * Removes plugin. It goes for {@code project/build/plugins[]}  matched by GA and removes it if present.
     */
    public boolean deletePlugin(Artifact artifact) {
        requireNonNull(artifact);
        Element build = editor.findChildElement(editor.root(), MavenPomElements.Elements.BUILD);
        if (build != null) {
            Element plugins = editor.findChildElement(build, MavenPomElements.Elements.PLUGINS);
            if (plugins != null) {
                Element plugin = plugins.children(MavenPomElements.Elements.PLUGIN)
                        .filter(predicatePluginGA(artifact))
                        .findFirst()
                        .orElse(null);
                if (plugin != null) {
                    return editor.removeElement(plugin);
                }
            }
        }
        return false;
    }

    /**
     * Updates/insert build extension. It goes for {@code project/build/extensions/extension[]} matched by GA.
     * If present, but version element is a property, it goes for {@link #updateProperty(boolean, String, String)}.
     */
    public boolean updateExtension(boolean upsert, Artifact artifact) {
        requireNonNull(artifact);
        Element build = editor.findChildElement(editor.root(), MavenPomElements.Elements.BUILD);
        if (build == null && upsert) {
            build = editor.insertMavenElement(editor.root(), MavenPomElements.Elements.BUILD);
        }
        if (build != null) {
            Element extensions = editor.findChildElement(build, MavenPomElements.Elements.EXTENSIONS);
            if (extensions == null && upsert) {
                extensions = editor.insertMavenElement(build, MavenPomElements.Elements.EXTENSIONS);
            }
            if (extensions != null) {
                // TODO: https://github.com/maveniverse/domtrip/issues/32
                Element extension = extensions
                        .children("extension")
                        .filter(predicateGA(artifact))
                        .findFirst()
                        .orElse(null);
                if (extension == null && upsert) {
                    // TODO: https://github.com/maveniverse/domtrip/issues/32
                    extension = editor.insertMavenElement(extensions, "extension");
                    editor.insertMavenElement(extension, MavenPomElements.Elements.GROUP_ID, artifact.getGroupId());
                    editor.insertMavenElement(
                            extension, MavenPomElements.Elements.ARTIFACT_ID, artifact.getArtifactId());
                    editor.insertMavenElement(extension, MavenPomElements.Elements.VERSION, artifact.getVersion());
                    return true;
                }
                if (extension != null) {
                    Optional<Element> version = extension.child(MavenPomElements.Elements.VERSION);
                    if (version.isPresent()) {
                        String versionValue = version.orElseThrow().textContent();
                        if (versionValue.startsWith("${") && versionValue.endsWith("}")) {
                            String propertyKey = versionValue.substring(2, versionValue.length() - 1);
                            return updateProperty(false, propertyKey, artifact.getVersion());
                        } else {
                            version.orElseThrow().textContent(artifact.getVersion());
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
    public boolean deleteExtension(Artifact artifact) {
        requireNonNull(artifact);
        Element build = editor.findChildElement(editor.root(), MavenPomElements.Elements.BUILD);
        if (build != null) {
            Element extensions = editor.findChildElement(build, MavenPomElements.Elements.EXTENSIONS);
            if (extensions != null) {
                // TODO: https://github.com/maveniverse/domtrip/issues/32
                Element extension = extensions
                        .children("extension")
                        .filter(predicateGA(artifact))
                        .findFirst()
                        .orElse(null);
                if (extension != null) {
                    return editor.removeElement(extension);
                }
            }
        }
        return false;
    }
}
