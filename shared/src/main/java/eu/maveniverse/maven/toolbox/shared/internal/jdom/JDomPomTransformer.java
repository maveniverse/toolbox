/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal.jdom;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import org.eclipse.aether.artifact.Artifact;
import org.jdom2.CDATA;
import org.jdom2.Comment;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.ContentFilter;
import org.jdom2.input.SAXBuilder;
import org.jdom2.located.LocatedJDOMFactory;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

/**
 * Construction to accept collection of artifacts, and applies it to some POM based on provided transformations.
 */
public final class JDomPomTransformer {
    /**
     * The transformation context.
     */
    public interface TransformationContext {
        boolean pomHasParent();

        Document getDocument();

        void registerPostTransformation(Consumer<TransformationContext> transformation);
    }

    /**
     * Removes empty remnant tags, like {@code <plugins />}.
     */
    private static final Consumer<TransformationContext> removeEmptyElements = ctx -> {
        // Remove empty elements
        for (String cleanUpEmptyElement : List.of(
                JDomCfg.POM_ELEMENT_MODULES,
                JDomCfg.POM_ELEMENT_PROPERTIES,
                JDomCfg.POM_ELEMENT_PLUGINS,
                JDomCfg.POM_ELEMENT_PLUGIN_MANAGEMENT,
                JDomCfg.POM_ELEMENT_DEPENDENCIES,
                JDomCfg.POM_ELEMENT_DEPENDENCY_MANAGEMENT)) {
            JDomCleanupHelper.cleanupEmptyElements(ctx.getDocument().getRootElement(), cleanUpEmptyElement);
        }
        // Remove empty (i.e. with no elements) profile and profiles tag
        JDomCleanupHelper.cleanupEmptyProfiles(
                ctx.getDocument().getRootElement(), List.of(JDomCfg.POM_ELEMENT_PROJECT));
    };

    /**
     * Helper for GA equality. To be used with plugins.
     */
    private static boolean equalsGA(Artifact artifact, Element element) {
        String groupId = element.getChildText("groupId", element.getNamespace());
        String artifactId = element.getChildText("artifactId", element.getNamespace());
        return Objects.equals(artifact.getGroupId(), groupId) && Objects.equals(artifact.getArtifactId(), artifactId);
    }

    /**
     * Helper for GATC equality. To be used with dependencies.
     */
    private static boolean equalsGATC(Artifact artifact, Element element) {
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

    private static Consumer<TransformationContext> addOrSetProperty(String key, String value) {
        return context -> {
            Element project = context.getDocument().getRootElement();
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
        };
    }

    public static Function<Artifact, Consumer<TransformationContext>> updateManagedPlugin(boolean upsert) {
        return a -> context -> {
            Element project = context.getDocument().getRootElement();
            if (project != null) {
                Element build = project.getChild("build", project.getNamespace());
                if (upsert && build == null) {
                    build = new Element("build", project.getNamespace());
                    JDomUtils.addElement(build, project);
                }
                if (build != null) {
                    Element pluginManagement = build.getChild("pluginManagement", project.getNamespace());
                    if (upsert && pluginManagement == null) {
                        pluginManagement = new Element("pluginManagement", project.getNamespace());
                        JDomUtils.addElement(pluginManagement, build);
                    }
                    if (pluginManagement != null) {
                        Element plugins = pluginManagement.getChild("plugins", pluginManagement.getNamespace());
                        if (upsert && plugins == null) {
                            plugins = new Element("plugins", pluginManagement.getNamespace());
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
                                JDomUtils.addElement(toUpdate, plugins);
                                JDomUtils.addElement(
                                        new Element("groupId", plugins.getNamespace()).setText(a.getGroupId()),
                                        toUpdate);
                                JDomUtils.addElement(
                                        new Element("artifactId", plugins.getNamespace()).setText(a.getArtifactId()),
                                        toUpdate);
                                JDomUtils.addElement(
                                        new Element("version", plugins.getNamespace()).setText(a.getVersion()),
                                        toUpdate);
                                return;
                            }
                            if (toUpdate != null) {
                                Element version = toUpdate.getChild("version", toUpdate.getNamespace());
                                if (version != null) {
                                    String versionValue = version.getText();
                                    if (versionValue.startsWith("${") && versionValue.endsWith("}")) {
                                        String propertyKey = versionValue.substring(2, versionValue.length() - 1);
                                        addOrSetProperty(propertyKey, a.getVersion())
                                                .accept(context);
                                    } else {
                                        version.setText(a.getVersion());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        };
    }

    public static Function<Artifact, Consumer<TransformationContext>> deleteManagedPlugin() {
        return a -> context -> {
            Element project = context.getDocument().getRootElement();
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
                                    context.registerPostTransformation(removeEmptyElements);
                                }
                            }
                        }
                    }
                }
            }
        };
    }

    public static Function<Artifact, Consumer<TransformationContext>> updatePlugin(boolean upsert) {
        return a -> context -> {
            Element project = context.getDocument().getRootElement();
            if (project != null) {
                Element build = project.getChild("build", project.getNamespace());
                if (upsert && build == null) {
                    build = new Element("build", project.getNamespace());
                    JDomUtils.addElement(build, project);
                }
                if (build != null) {
                    Element plugins = build.getChild("plugins", build.getNamespace());
                    if (upsert && plugins == null) {
                        plugins = new Element("plugins", build.getNamespace());
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
                            Element version = toUpdate.getChild("version", plugins.getNamespace());
                            if (version != null) {
                                String versionValue = version.getText();
                                if (versionValue.startsWith("${") && versionValue.endsWith("}")) {
                                    String propertyKey = versionValue.substring(2, versionValue.length() - 1);
                                    addOrSetProperty(propertyKey, a.getVersion())
                                            .accept(context);
                                } else {
                                    version.setText(a.getVersion());
                                }
                            } else {
                                updateManagedPlugin(upsert).apply(a).accept(context);
                            }
                        }
                    }
                }
            }
        };
    }

    public static Function<Artifact, Consumer<TransformationContext>> deletePlugin() {
        return a -> context -> {
            Element project = context.getDocument().getRootElement();
            if (project != null) {
                Element plugins = project.getChild("plugins", project.getNamespace());
                if (plugins != null) {
                    for (Element plugin : plugins.getChildren("plugin", plugins.getNamespace())) {
                        if (equalsGA(a, plugin)) {
                            JDomUtils.removeChildAndItsCommentFromContent(plugins, plugin);
                            context.registerPostTransformation(removeEmptyElements);
                        }
                    }
                }
            }
        };
    }

    public static Function<Artifact, Consumer<TransformationContext>> updateManagedDependency(boolean upsert) {
        return a -> context -> {
            Element project = context.getDocument().getRootElement();
            if (project != null) {
                Element dependencyManagement = project.getChild("dependencyManagement", project.getNamespace());
                if (upsert && dependencyManagement == null) {
                    dependencyManagement = new Element("dependencyManagement", project.getNamespace());
                    JDomUtils.addElement(dependencyManagement, project);
                }
                if (dependencyManagement != null) {
                    Element dependencies = dependencyManagement.getChild("dependencies", project.getNamespace());
                    if (upsert && dependencies == null) {
                        dependencies = new Element("dependencies", project.getNamespace());
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
                            JDomUtils.addElement(toUpdate, dependencies);
                            JDomUtils.addElement(
                                    new Element("groupId", dependencies.getNamespace()).setText(a.getGroupId()),
                                    toUpdate);
                            JDomUtils.addElement(
                                    new Element("artifactId", dependencies.getNamespace()).setText(a.getArtifactId()),
                                    toUpdate);
                            JDomUtils.addElement(
                                    new Element("version", dependencies.getNamespace()).setText(a.getVersion()),
                                    toUpdate);
                            if (!"jar".equals(a.getExtension())) {
                                JDomUtils.addElement(
                                        new Element("type", dependencies.getNamespace()).setText(a.getExtension()),
                                        toUpdate);
                            }
                            if (!a.getClassifier().isEmpty()) {
                                JDomUtils.addElement(
                                        new Element("classifier", dependencies.getNamespace())
                                                .setText(a.getClassifier()),
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
                                    addOrSetProperty(propertyKey, a.getVersion())
                                            .accept(context);
                                } else {
                                    version.setText(a.getVersion());
                                }
                            } else {
                                updateManagedDependency(upsert).apply(a).accept(context);
                            }
                        }
                    }
                }
            }
        };
    }

    public static Function<Artifact, Consumer<TransformationContext>> deleteManagedDependency() {
        return a -> context -> {
            Element project = context.getDocument().getRootElement();
            if (project != null) {
                Element dependencyManagement = project.getChild("dependencyManagement", project.getNamespace());
                if (dependencyManagement != null) {
                    Element dependencies =
                            dependencyManagement.getChild("dependencies", dependencyManagement.getNamespace());
                    if (dependencies != null) {
                        for (Element dependency : dependencies.getChildren("dependency", dependencies.getNamespace())) {
                            if (equalsGATC(a, dependency)) {
                                JDomUtils.removeChildAndItsCommentFromContent(dependencies, dependency);
                                context.registerPostTransformation(removeEmptyElements);
                            }
                        }
                    }
                }
            }
        };
    }

    public static Function<Artifact, Consumer<TransformationContext>> updateDependency(boolean upsert) {
        return a -> context -> {
            Element project = context.getDocument().getRootElement();
            if (project != null) {
                Element dependencies = project.getChild("dependencies", project.getNamespace());
                if (upsert && dependencies == null) {
                    dependencies = new Element("dependencies", project.getNamespace());
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
                                addOrSetProperty(propertyKey, a.getVersion()).accept(context);
                            } else {
                                version.setText(a.getVersion());
                            }
                        } else {
                            updateManagedDependency(upsert).apply(a).accept(context);
                        }
                    }
                }
            }
        };
    }

    public static Function<Artifact, Consumer<TransformationContext>> deleteDependency() {
        return a -> context -> {
            Element project = context.getDocument().getRootElement();
            if (project != null) {
                Element dependencies = project.getChild("dependencies", project.getNamespace());
                if (dependencies != null) {
                    for (Element dependency : dependencies.getChildren("dependency", dependencies.getNamespace())) {
                        if (equalsGATC(a, dependency)) {
                            JDomUtils.removeChildAndItsCommentFromContent(dependencies, dependency);
                            context.registerPostTransformation(removeEmptyElements);
                        }
                    }
                }
            }
        };
    }

    public void apply(Path pom, List<Consumer<TransformationContext>> transformations) throws IOException {
        requireNonNull(pom, "pom");
        requireNonNull(transformations, "transformations");
        if (!transformations.isEmpty()) {
            String head = null;
            String body;
            String tail = null;

            body = Files.readString(pom, StandardCharsets.UTF_8);
            body = normalizeLineEndings(body, System.lineSeparator());

            SAXBuilder builder = new SAXBuilder();
            builder.setJDOMFactory(new LocatedJDOMFactory());
            Document document;
            try {
                document = builder.build(new StringReader(body));
            } catch (JDOMException e) {
                throw new IOException(e);
            }
            normaliseLineEndings(document, System.lineSeparator());

            int headIndex = body.indexOf("<" + document.getRootElement().getName());
            if (headIndex >= 0) {
                head = body.substring(0, headIndex);
            }
            String lastTag = "</" + document.getRootElement().getName() + ">";
            int tailIndex = body.lastIndexOf(lastTag);
            if (tailIndex >= 0) {
                tail = body.substring(tailIndex + lastTag.length());
            }

            ArrayList<Consumer<TransformationContext>> postProcessors = new ArrayList<>();
            TransformationContext context = new TransformationContext() {
                @Override
                public boolean pomHasParent() {
                    return document.getRootElement()
                                    .getChild(
                                            "parent", document.getRootElement().getNamespace())
                            != null;
                }

                @Override
                public Document getDocument() {
                    return document;
                }

                @Override
                public void registerPostTransformation(Consumer<TransformationContext> transformation) {
                    postProcessors.add(transformation);
                }
            };
            for (Consumer<TransformationContext> transformation : transformations) {
                transformation.accept(context);
            }
            for (Consumer<TransformationContext> transformation : postProcessors) {
                transformation.accept(context);
            }

            Format format = Format.getRawFormat();
            format.setLineSeparator(System.lineSeparator());
            XMLOutputter out = new XMLOutputter(format);
            try (OutputStream outputStream = Files.newOutputStream(pom)) {
                if (head != null) {
                    outputStream.write(head.getBytes(StandardCharsets.UTF_8));
                }
                out.output(document.getRootElement(), outputStream);
                if (tail != null) {
                    outputStream.write(tail.getBytes(StandardCharsets.UTF_8));
                }
                outputStream.flush();
            }
        }
    }

    private static void normaliseLineEndings(Document document, String lineSeparator) {
        for (Iterator<?> i = document.getDescendants(new ContentFilter(ContentFilter.COMMENT)); i.hasNext(); ) {
            Comment c = (Comment) i.next();
            c.setText(normalizeLineEndings(c.getText(), lineSeparator));
        }
        for (Iterator<?> i = document.getDescendants(new ContentFilter(ContentFilter.CDATA)); i.hasNext(); ) {
            CDATA c = (CDATA) i.next();
            c.setText(normalizeLineEndings(c.getText(), lineSeparator));
        }
    }

    private static String normalizeLineEndings(String text, String separator) {
        String norm = text;
        if (text != null) {
            norm = text.replaceAll("(\r\n)|(\n)|(\r)", separator);
        }
        return norm;
    }
}
