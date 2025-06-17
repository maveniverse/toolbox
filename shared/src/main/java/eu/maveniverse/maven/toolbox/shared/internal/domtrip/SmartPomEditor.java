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
import static java.util.Objects.requireNonNull;
import static org.maveniverse.domtrip.maven.MavenPomElements.Elements.ARTIFACT_ID;
import static org.maveniverse.domtrip.maven.MavenPomElements.Elements.GROUP_ID;
import static org.maveniverse.domtrip.maven.MavenPomElements.Elements.VERSION;

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

    public void setPackaging(String value) {
        requireNonNull(value);
        Element packaging = editor.findChildElement(editor.root(), MavenPomElements.Elements.PACKAGING);
        if (packaging == null) {
            editor.insertMavenElement(editor.root(), MavenPomElements.Elements.PACKAGING, value);
        } else {
            packaging.textContent(value);
        }
    }

    public boolean addSubProject(String value) {
        requireNonNull(value);
        Element modules = editor.findChildElement(editor.root(), MavenPomElements.Elements.MODULES);
        if (modules == null) {
            modules = editor.insertMavenElement(editor.root(), MavenPomElements.Elements.MODULES);
        }
        List<String> existing = modules.descendants(MavenPomElements.Elements.MODULE)
                .map(Element::textContent)
                .toList();
        if (!existing.contains(value)) {
            Element module = editor.insertMavenElement(modules, MavenPomElements.Elements.MODULE);
            module.textContent(value);
            return true;
        }
        return false;
    }

    public boolean removeSubProject(String value) {
        requireNonNull(value);
        Element modules = editor.findChildElement(editor.root(), MavenPomElements.Elements.MODULES);
        if (modules == null) {
            return false;
        }
        AtomicBoolean removed = new AtomicBoolean(false);
        modules.descendants(MavenPomElements.Elements.MODULE)
                .filter(e -> Objects.equals(value, e.textContent()))
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
                        .descendants(MavenPomElements.Elements.DEPENDENCY)
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
                    Optional<Element> version = dependency.descendant(MavenPomElements.Elements.VERSION);
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

    public boolean deleteManagedDependency(Artifact artifact) {
        requireNonNull(artifact);
        Element dependencyManagement =
                editor.findChildElement(editor.root(), MavenPomElements.Elements.DEPENDENCY_MANAGEMENT);
        if (dependencyManagement != null) {
            Element dependencies =
                    editor.findChildElement(dependencyManagement, MavenPomElements.Elements.DEPENDENCIES);
            if (dependencies != null) {
                Element dependency = dependencies
                        .descendants(MavenPomElements.Elements.DEPENDENCY)
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

    public boolean updateDependency(boolean upsert, Artifact artifact) {
        requireNonNull(artifact);
        Element dependencies = editor.findChildElement(editor.root(), MavenPomElements.Elements.DEPENDENCIES);
        if (dependencies == null && upsert) {
            dependencies = editor.insertMavenElement(editor.root(), MavenPomElements.Elements.DEPENDENCIES);
        }
        if (dependencies != null) {
            Element dependency = dependencies
                    .descendants(MavenPomElements.Elements.DEPENDENCY)
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
                Optional<Element> version = dependency.descendant(MavenPomElements.Elements.VERSION);
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
                    return updateManagedDependency(upsert, artifact);
                }
            }
        }
        return false;
    }

    public boolean deleteDependency(Artifact artifact) {
        requireNonNull(artifact);
        Element dependencies = editor.findChildElement(editor.root(), MavenPomElements.Elements.DEPENDENCIES);
        if (dependencies != null) {
            Element dependency = dependencies
                    .descendants(MavenPomElements.Elements.DEPENDENCY)
                    .filter(predicateGATC(artifact))
                    .findFirst()
                    .orElse(null);
            if (dependency != null) {
                return editor.removeElement(dependency);
            }
        }
        return false;
    }

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
                    Element plugin = plugins.descendants(MavenPomElements.Elements.PLUGIN)
                            .filter(predicateGA(artifact))
                            .findFirst()
                            .orElse(null);
                    if (plugin == null && upsert) {
                        editor.addPlugin(
                                plugins, artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
                        return true;
                    }
                    if (plugin != null) {
                        Optional<Element> version = plugin.descendant(MavenPomElements.Elements.VERSION);
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

    public boolean deleteManagedPlugin(Artifact artifact) {
        requireNonNull(artifact);
        Element build = editor.findChildElement(editor.root(), MavenPomElements.Elements.BUILD);
        if (build != null) {
            Element pluginManagement = editor.findChildElement(build, MavenPomElements.Elements.PLUGIN_MANAGEMENT);
            if (pluginManagement != null) {
                Element plugins = editor.findChildElement(pluginManagement, MavenPomElements.Elements.PLUGINS);
                if (plugins != null) {
                    Element plugin = plugins.descendants(MavenPomElements.Elements.PLUGIN)
                            .filter(predicateGA(artifact))
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
                Element plugin = plugins.descendants(MavenPomElements.Elements.PLUGIN)
                        .filter(predicateGA(artifact))
                        .findFirst()
                        .orElse(null);
                if (plugin == null && upsert) {
                    editor.addPlugin(plugins, artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
                    return true;
                }
                if (plugin != null) {
                    Optional<Element> version = plugin.descendant(MavenPomElements.Elements.VERSION);
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

    public boolean deletePlugin(Artifact artifact) {
        requireNonNull(artifact);
        Element build = editor.findChildElement(editor.root(), MavenPomElements.Elements.BUILD);
        if (build != null) {
            Element plugins = editor.findChildElement(build, MavenPomElements.Elements.PLUGINS);
            if (plugins != null) {
                Element plugin = plugins.descendants(MavenPomElements.Elements.PLUGIN)
                        .filter(predicateGA(artifact))
                        .findFirst()
                        .orElse(null);
                if (plugin != null) {
                    return editor.removeElement(plugin);
                }
            }
        }
        return false;
    }

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
                        .descendants("extension")
                        .filter(predicateGA(artifact))
                        .findFirst()
                        .orElse(null);
                if (extension == null && upsert) {
                    // TODO: https://github.com/maveniverse/domtrip/issues/32
                    extension = editor.insertMavenElement(extensions, "extension");
                    editor.insertMavenElement(extension, GROUP_ID, artifact.getGroupId());
                    editor.insertMavenElement(extension, ARTIFACT_ID, artifact.getArtifactId());
                    editor.insertMavenElement(extension, VERSION, artifact.getVersion());
                    return true;
                }
                if (extension != null) {
                    Optional<Element> version = extension.descendant(MavenPomElements.Elements.VERSION);
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

    public boolean deleteExtension(Artifact artifact) {
        requireNonNull(artifact);
        Element build = editor.findChildElement(editor.root(), MavenPomElements.Elements.BUILD);
        if (build != null) {
            Element extensions = editor.findChildElement(build, MavenPomElements.Elements.EXTENSIONS);
            if (extensions != null) {
                // TODO: https://github.com/maveniverse/domtrip/issues/32
                Element extension = extensions
                        .descendants("extension")
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
