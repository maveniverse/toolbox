/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal.jdom;

import java.util.Objects;
import org.eclipse.aether.artifact.Artifact;
import org.jdom2.Element;
import org.jdom2.Text;

/**
 * Methods editing JDom POM representation.
 */
public final class JDomPomEditor {
    /**
     * Helper for GA equality. To be used with plugins.
     */
    public static boolean equalsGA(Artifact artifact, Element element) {
        String groupId = element.getChildText("groupId", element.getNamespace());
        String artifactId = element.getChildText("artifactId", element.getNamespace());
        return Objects.equals(artifact.getGroupId(), groupId) && Objects.equals(artifact.getArtifactId(), artifactId);
    }

    /**
     * Helper for GATC equality. To be used with dependencies.
     */
    public static boolean equalsGATC(Artifact artifact, Element element) {
        String groupId = element.getChildText("groupId", element.getNamespace());
        String artifactId = element.getChildText("artifactId", element.getNamespace());
        String type = element.getChildText("type", element.getNamespace());
        if (type == null) {
            type = "jar";
        }
        String classifier = element.getChildText("classifier", element.getNamespace());
        if (classifier == null) {
            classifier = "";
        }
        return Objects.equals(artifact.getGroupId(), groupId)
                && Objects.equals(artifact.getArtifactId(), artifactId)
                && Objects.equals(artifact.getClassifier(), classifier)
                && Objects.equals(artifact.getExtension(), type);
    }

    public static void addOrSetProperty(Element project, String key, String value) {
        if (project != null) {
            Element properties = project.getChild("properties", project.getNamespace());
            if (properties == null) {
                properties = new Element("properties", project.getNamespace());
                JDomUtils.addElement(properties, project);
            }
            Element property = properties.getChild(key, properties.getNamespace());
            if (property == null) {
                property = new Element(key, properties.getNamespace());
                JDomUtils.addElement(property, properties);
            }
            property.setText(value);
        }
    }

    public static void setParent(Element project, Artifact a) {
        if (project != null) {
            Element parent = project.getChild("parent", project.getNamespace());
            if (parent == null) {
                parent = new Element("parent", project.getNamespace());
                parent.addContent(new Text("\n  " + JDomUtils.detectIndentation(project)));
                JDomUtils.addElement(parent, project);
            }
            JDomUtils.rewriteElement("groupId", a.getGroupId(), parent);
            JDomUtils.rewriteElement("artifactId", a.getArtifactId(), parent);
            JDomUtils.rewriteElement("version", a.getVersion(), parent);

            Element groupId = project.getChild("groupId", project.getNamespace());
            if (groupId != null && groupId.getText().equals(a.getGroupId())) {
                JDomUtils.removeChildElement(project, groupId);
            }

            Element version = project.getChild("version", project.getNamespace());
            if (version != null && version.getText().equals(a.getVersion())) {
                JDomUtils.removeChildElement(project, version);
            }
        }
    }

    public static void addSubProject(Element project, String subproject) {
        if (project != null) {
            Element modules = project.getChild("modules", project.getNamespace());
            if (modules == null) {
                modules = new Element("modules", project.getNamespace());
                modules.addContent(new Text("\n  " + JDomUtils.detectIndentation(project)));
                JDomUtils.addElement(modules, project);
            }
            Element module = new Element("module", modules.getNamespace());
            module.setText(subproject);
            JDomUtils.addElement(module, modules);
        }
    }

    public static void setPackaging(Element project, String value) {
        if (project != null) {
            Element packaging = project.getChild("packaging", project.getNamespace());
            if (packaging == null) {
                packaging = new Element("packaging", project.getNamespace());
                JDomUtils.addElement(packaging, project);
            }
            if ("jar".equals(value)) {
                JDomUtils.removeChildAndItsCommentFromContent(project, packaging);
            } else {
                packaging.setText(value);
            }
        }
    }

    public static void updateManagedPlugin(Element project, Artifact a, boolean upsert) {
        if (project != null) {
            Element build = project.getChild("build", project.getNamespace());
            if (upsert && build == null) {
                build = new Element("build", project.getNamespace());
                build.addContent(new Text("\n  " + JDomUtils.detectIndentation(project)));
                JDomUtils.addElement(build, project);
            }
            if (build != null) {
                Element pluginManagement = build.getChild("pluginManagement", build.getNamespace());
                if (upsert && pluginManagement == null) {
                    pluginManagement = new Element("pluginManagement", build.getNamespace());
                    pluginManagement.addContent(new Text("\n  " + JDomUtils.detectIndentation(build)));
                    JDomUtils.addElement(pluginManagement, build);
                }
                if (pluginManagement != null) {
                    Element plugins = pluginManagement.getChild("plugins", pluginManagement.getNamespace());
                    if (upsert && plugins == null) {
                        plugins = new Element("plugins", pluginManagement.getNamespace());
                        plugins.addContent(new Text("\n  " + JDomUtils.detectIndentation(pluginManagement)));
                        JDomUtils.addElement(plugins, pluginManagement);
                    }
                    if (plugins != null) {
                        Element toUpdate = null;
                        for (Element plugin : plugins.getChildren("plugin", plugins.getNamespace())) {
                            if (equalsGA(a, plugin)) {
                                toUpdate = plugin;
                                break;
                            }
                        }
                        if (upsert && toUpdate == null) {
                            toUpdate = new Element("plugin", plugins.getNamespace());
                            toUpdate.addContent(new Text("\n  " + JDomUtils.detectIndentation(plugins)));
                            JDomUtils.addElement(toUpdate, plugins);
                            JDomUtils.addElement(
                                    new Element("groupId", plugins.getNamespace()).setText(a.getGroupId()), toUpdate);
                            JDomUtils.addElement(
                                    new Element("artifactId", plugins.getNamespace()).setText(a.getArtifactId()),
                                    toUpdate);
                            JDomUtils.addElement(
                                    new Element("version", plugins.getNamespace()).setText(a.getVersion()), toUpdate);
                            return;
                        }
                        if (toUpdate != null) {
                            Element version = toUpdate.getChild("version", toUpdate.getNamespace());
                            if (version != null) {
                                String versionValue = version.getText();
                                if (versionValue.startsWith("${") && versionValue.endsWith("}")) {
                                    String propertyKey = versionValue.substring(2, versionValue.length() - 1);
                                    addOrSetProperty(project, propertyKey, a.getVersion());
                                } else {
                                    version.setText(a.getVersion());
                                }
                            }
                        }
                    }
                }
            }
        }
        ;
    }

    public static void deleteManagedPlugin(Element project, Artifact a) {
        if (project != null) {
            Element build = project.getChild("build", project.getNamespace());
            if (build != null) {
                Element pluginManagement = project.getChild("pluginManagement", project.getNamespace());
                if (pluginManagement != null) {
                    Element plugins = pluginManagement.getChild("plugins", pluginManagement.getNamespace());
                    if (plugins != null) {
                        for (Element plugin : plugins.getChildren("plugin", plugins.getNamespace())) {
                            if (equalsGA(a, plugin)) {
                                JDomUtils.removeChildAndItsCommentFromContent(plugins, plugin);
                            }
                        }
                    }
                }
            }
        }
    }

    public static void updatePlugin(Element project, Artifact a, boolean upsert) {
        if (project != null) {
            Element build = project.getChild("build", project.getNamespace());
            if (upsert && build == null) {
                build = new Element("build", project.getNamespace());
                build.addContent(new Text("\n  " + JDomUtils.detectIndentation(project)));
                JDomUtils.addElement(build, project);
            }
            if (build != null) {
                Element plugins = build.getChild("plugins", build.getNamespace());
                if (upsert && plugins == null) {
                    plugins = new Element("plugins", build.getNamespace());
                    plugins.addContent(new Text("\n  " + JDomUtils.detectIndentation(build)));
                    JDomUtils.addElement(plugins, build);
                }
                if (plugins != null) {
                    Element toUpdate = null;
                    for (Element plugin : plugins.getChildren("plugin", plugins.getNamespace())) {
                        if (equalsGA(a, plugin)) {
                            toUpdate = plugin;
                            break;
                        }
                    }
                    if (upsert && toUpdate == null) {
                        toUpdate = new Element("plugin", plugins.getNamespace());
                        toUpdate.addContent(new Text("\n  " + JDomUtils.detectIndentation(plugins)));
                        JDomUtils.addElement(toUpdate, plugins);
                        JDomUtils.addElement(
                                new Element("groupId", plugins.getNamespace()).setText(a.getGroupId()), toUpdate);
                        JDomUtils.addElement(
                                new Element("artifactId", plugins.getNamespace()).setText(a.getArtifactId()), toUpdate);
                        JDomUtils.addElement(
                                new Element("version", plugins.getNamespace()).setText(a.getVersion()), toUpdate);
                        return;
                    }
                    if (toUpdate != null) {
                        Element version = toUpdate.getChild("version", plugins.getNamespace());
                        if (version != null) {
                            String versionValue = version.getText();
                            if (versionValue.startsWith("${") && versionValue.endsWith("}")) {
                                String propertyKey = versionValue.substring(2, versionValue.length() - 1);
                                addOrSetProperty(project, propertyKey, a.getVersion());
                            } else {
                                version.setText(a.getVersion());
                            }
                        } else {
                            updateManagedPlugin(project, a, upsert);
                        }
                    }
                }
            }
        }
    }

    public static void deletePlugin(Element project, Artifact a) {
        if (project != null) {
            Element plugins = project.getChild("plugins", project.getNamespace());
            if (plugins != null) {
                for (Element plugin : plugins.getChildren("plugin", plugins.getNamespace())) {
                    if (equalsGA(a, plugin)) {
                        JDomUtils.removeChildAndItsCommentFromContent(plugins, plugin);
                    }
                }
            }
        }
    }

    public static void updateManagedDependency(Element project, Artifact a, boolean upsert) {
        if (project != null) {
            Element dependencyManagement = project.getChild("dependencyManagement", project.getNamespace());
            if (upsert && dependencyManagement == null) {
                dependencyManagement = new Element("dependencyManagement", project.getNamespace());
                dependencyManagement.addContent(new Text("\n  " + JDomUtils.detectIndentation(project)));
                JDomUtils.addElement(dependencyManagement, project);
            }
            if (dependencyManagement != null) {
                Element dependencies =
                        dependencyManagement.getChild("dependencies", dependencyManagement.getNamespace());
                if (upsert && dependencies == null) {
                    dependencies = new Element("dependencies", dependencyManagement.getNamespace());
                    dependencies.addContent(new Text("\n  " + JDomUtils.detectIndentation(dependencyManagement)));
                    JDomUtils.addElement(dependencies, dependencyManagement);
                }
                if (dependencies != null) {
                    Element toUpdate = null;
                    for (Element dependency : dependencies.getChildren("dependency", dependencies.getNamespace())) {
                        if (equalsGATC(a, dependency)) {
                            toUpdate = dependency;
                            break;
                        }
                    }
                    if (upsert && toUpdate == null) {
                        toUpdate = new Element("dependency", dependencies.getNamespace());
                        toUpdate.addContent(new Text("\n  " + JDomUtils.detectIndentation(dependencies)));
                        JDomUtils.addElement(toUpdate, dependencies);
                        JDomUtils.addElement(
                                new Element("groupId", dependencies.getNamespace()).setText(a.getGroupId()), toUpdate);
                        JDomUtils.addElement(
                                new Element("artifactId", dependencies.getNamespace()).setText(a.getArtifactId()),
                                toUpdate);
                        JDomUtils.addElement(
                                new Element("version", dependencies.getNamespace()).setText(a.getVersion()), toUpdate);
                        if (!"jar".equals(a.getExtension())) {
                            JDomUtils.addElement(
                                    new Element("type", dependencies.getNamespace()).setText(a.getExtension()),
                                    toUpdate);
                        }
                        if (!a.getClassifier().isEmpty()) {
                            JDomUtils.addElement(
                                    new Element("classifier", dependencies.getNamespace()).setText(a.getClassifier()),
                                    toUpdate);
                        }
                        return;
                    }
                    if (toUpdate != null) {
                        Element version = toUpdate.getChild("version", dependencies.getNamespace());
                        if (version != null) {
                            String versionValue = version.getText();
                            if (versionValue.startsWith("${") && versionValue.endsWith("}")) {
                                String propertyKey = versionValue.substring(2, versionValue.length() - 1);
                                addOrSetProperty(project, propertyKey, a.getVersion());
                            } else {
                                version.setText(a.getVersion());
                            }
                        } else {
                            updateManagedDependency(project, a, upsert);
                        }
                    }
                }
            }
        }
    }

    public static void deleteManagedDependency(Element project, Artifact a) {
        if (project != null) {
            Element dependencyManagement = project.getChild("dependencyManagement", project.getNamespace());
            if (dependencyManagement != null) {
                Element dependencies =
                        dependencyManagement.getChild("dependencies", dependencyManagement.getNamespace());
                if (dependencies != null) {
                    for (Element dependency : dependencies.getChildren("dependency", dependencies.getNamespace())) {
                        if (equalsGATC(a, dependency)) {
                            JDomUtils.removeChildAndItsCommentFromContent(dependencies, dependency);
                        }
                    }
                }
            }
        }
    }

    public static void updateDependency(Element project, Artifact a, boolean upsert) {
        if (project != null) {
            Element dependencies = project.getChild("dependencies", project.getNamespace());
            if (upsert && dependencies == null) {
                dependencies = new Element("dependencies", project.getNamespace());
                dependencies.addContent(new Text("\n  " + JDomUtils.detectIndentation(project)));
                JDomUtils.addElement(dependencies, project);
            }
            if (dependencies != null) {
                Element toUpdate = null;
                for (Element dependency : dependencies.getChildren("dependency", dependencies.getNamespace())) {
                    if (equalsGATC(a, dependency)) {
                        toUpdate = dependency;
                        break;
                    }
                }
                if (upsert && toUpdate == null) {
                    toUpdate = new Element("dependency", dependencies.getNamespace());
                    toUpdate.addContent(new Text("\n  " + JDomUtils.detectIndentation(dependencies)));
                    JDomUtils.addElement(toUpdate, dependencies);
                    JDomUtils.addElement(
                            new Element("groupId", dependencies.getNamespace()).setText(a.getGroupId()), toUpdate);
                    JDomUtils.addElement(
                            new Element("artifactId", dependencies.getNamespace()).setText(a.getArtifactId()),
                            toUpdate);
                    JDomUtils.addElement(
                            new Element("version", dependencies.getNamespace()).setText(a.getVersion()), toUpdate);
                    if (!"jar".equals(a.getExtension())) {
                        JDomUtils.addElement(
                                new Element("type", dependencies.getNamespace()).setText(a.getExtension()), toUpdate);
                    }
                    if (!a.getClassifier().isEmpty()) {
                        JDomUtils.addElement(
                                new Element("classifier", dependencies.getNamespace()).setText(a.getClassifier()),
                                toUpdate);
                    }
                    return;
                }
                if (toUpdate != null) {
                    Element version = toUpdate.getChild("version", dependencies.getNamespace());
                    if (version != null) {
                        String versionValue = version.getText();
                        if (versionValue.startsWith("${") && versionValue.endsWith("}")) {
                            String propertyKey = versionValue.substring(2, versionValue.length() - 1);
                            addOrSetProperty(project, propertyKey, a.getVersion());
                        } else {
                            version.setText(a.getVersion());
                        }
                    } else {
                        updateManagedDependency(project, a, upsert);
                    }
                }
            }
        }
    }

    public static void deleteDependency(Element project, Artifact a) {
        if (project != null) {
            Element dependencies = project.getChild("dependencies", project.getNamespace());
            if (dependencies != null) {
                for (Element dependency : dependencies.getChildren("dependency", dependencies.getNamespace())) {
                    if (equalsGATC(a, dependency)) {
                        JDomUtils.removeChildAndItsCommentFromContent(dependencies, dependency);
                    }
                }
            }
        }
    }
}
