/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.toolbox.shared.ArtifactMapper;
import eu.maveniverse.maven.toolbox.shared.ArtifactMatcher;
import eu.maveniverse.maven.toolbox.shared.output.Output;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
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
public final class PomTransformerSink implements Artifacts.Sink {
    /**
     * A transformation to apply.
     */
    public interface Transformation extends Consumer<TransformationContext> {}

    /**
     * The transformation context.
     */
    public interface TransformationContext {
        Document getDocument();
    }

    /**
     * Removes empty remnant tags, like {@code <plugins />}.
     */
    private final Transformation removeEmptyElements = ctx -> {};

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
                    project.addContent(properties);
                }
                Element property = properties.getChild(key, properties.getNamespace());
                if (property == null) {
                    property = new Element(key, properties.getNamespace());
                    properties.addContent(property);
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
                    project.addContent(project.addContent(build));
                }
                if (build != null) {
                    Element pluginManagement = build.getChild("pluginManagement", project.getNamespace());
                    if (upsert && pluginManagement == null) {
                        pluginManagement = new Element("pluginManagement", project.getNamespace());
                        build.addContent(pluginManagement);
                    }
                    if (pluginManagement != null) {
                        Element plugins = pluginManagement.getChild("plugins", pluginManagement.getNamespace());
                        if (upsert && plugins == null) {
                            plugins = new Element("plugins", pluginManagement.getNamespace());
                            pluginManagement.addContent(plugins);
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
                                toUpdate.addContent(
                                        new Element("groupId", plugins.getNamespace()).setText(a.getGroupId()));
                                toUpdate.addContent(
                                        new Element("artifactId", plugins.getNamespace()).setText(a.getArtifactId()));
                                toUpdate.addContent(
                                        new Element("version", plugins.getNamespace()).setText(a.getVersion()));
                                plugins.addContent(toUpdate);
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
                                    plugin.detach();
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
                    project.addContent(project.addContent(build));
                }
                if (build != null) {
                    Element plugins = build.getChild("plugins", build.getNamespace());
                    if (upsert && plugins == null) {
                        plugins = new Element("plugins", build.getNamespace());
                        build.addContent(plugins);
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
                            toUpdate.addContent(new Element("groupId", plugins.getNamespace()).setText(a.getGroupId()));
                            toUpdate.addContent(
                                    new Element("artifactId", plugins.getNamespace()).setText(a.getArtifactId()));
                            toUpdate.addContent(new Element("version", plugins.getNamespace()).setText(a.getVersion()));
                            plugins.addContent(toUpdate);
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
                            plugin.detach();
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
                    project.addContent(dependencyManagement);
                }
                if (dependencyManagement != null) {
                    Element dependencies = dependencyManagement.getChild("dependencies", project.getNamespace());
                    if (upsert && dependencies == null) {
                        dependencies = new Element("dependencies", project.getNamespace());
                        dependencyManagement.addContent(dependencies);
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
                            toUpdate.addContent(
                                    new Element("groupId", dependencies.getNamespace()).setText(a.getGroupId()));
                            toUpdate.addContent(
                                    new Element("artifactId", dependencies.getNamespace()).setText(a.getArtifactId()));
                            toUpdate.addContent(
                                    new Element("version", dependencies.getNamespace()).setText(a.getVersion()));
                            if (!"jar".equals(a.getExtension())) {
                                toUpdate.addContent(
                                        new Element("type", dependencies.getNamespace()).setText(a.getExtension()));
                            }
                            if (!a.getClassifier().isEmpty()) {
                                toUpdate.addContent(new Element("classifier", dependencies.getNamespace())
                                        .setText(a.getClassifier()));
                            }
                            dependencies.addContent(toUpdate);
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
                                dependency.detach();
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
                    project.addContent(dependencies);
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
                        toUpdate.addContent(
                                new Element("groupId", dependencies.getNamespace()).setText(a.getGroupId()));
                        toUpdate.addContent(
                                new Element("artifactId", dependencies.getNamespace()).setText(a.getArtifactId()));
                        toUpdate.addContent(
                                new Element("version", dependencies.getNamespace()).setText(a.getVersion()));
                        if (!"jar".equals(a.getExtension())) {
                            toUpdate.addContent(
                                    new Element("type", dependencies.getNamespace()).setText(a.getExtension()));
                        }
                        if (!a.getClassifier().isEmpty()) {
                            toUpdate.addContent(
                                    new Element("classifier", dependencies.getNamespace()).setText(a.getClassifier()));
                        }
                        dependencies.addContent(toUpdate);
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
                            dependency.detach();
                        }
                    }
                }
            }
        };
    }

    /**
     * Creates trivial "transform" sink, that accepts all artifacts and applies provided transformations to artifacts as-is.
     * If no POM file exists, will provide a plain/trivial "blank" POM.
     */
    public static PomTransformerSink transform(
            Output output, Path pom, Function<Artifact, Consumer<TransformationContext>> transformations)
            throws IOException {
        return transform(
                output, pom, () -> BLANK_POM, ArtifactMatcher.any(), ArtifactMapper.identity(), transformations);
    }

    /**
     * Creates "transform" sink, fully customizable.
     */
    public static PomTransformerSink transform(
            Output output,
            Path pom,
            Supplier<String> pomSupplier,
            Predicate<Artifact> artifactMatcher,
            Function<Artifact, Artifact> artifactMapper,
            Function<Artifact, Consumer<TransformationContext>> transformations)
            throws IOException {
        return new PomTransformerSink(output, pom, pomSupplier, artifactMatcher, artifactMapper, transformations);
    }

    private static final String BLANK_POM = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" //
            + "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" //
            + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" //
            + "    <modelVersion>4.0.0</modelVersion>\n" //
            + "    <groupId>org.acme</groupId>\n" //
            + "    <artifactId>pom</artifactId>\n" //
            + "    <version>1.0-SNAPSHOT</version>\n" //
            + "    <packaging>pom</packaging>\n" //
            + "</project>\n";

    private final Output output;
    private final Path pom;
    private final Predicate<Artifact> artifactMatcher;
    private final Function<Artifact, Artifact> artifactMapper;
    private final Function<Artifact, Consumer<TransformationContext>> transformations;

    private final ArrayList<Consumer<TransformationContext>> applicableTransformations;

    /**
     * Creates a directory sink.
     *
     * @param output The output.
     * @param pom The POM path, if not existing, will be created (as "blank").
     * @param blankPomSupplier Required, if pom path points to a non-existent POM file.
     * @param artifactMatcher The artifact matcher.
     * @param artifactMapper The artifact mapper.
     * @param transformations The transformations to apply.
     * @throws IOException In case of IO problem.
     */
    private PomTransformerSink(
            Output output,
            Path pom,
            Supplier<String> blankPomSupplier,
            Predicate<Artifact> artifactMatcher,
            Function<Artifact, Artifact> artifactMapper,
            Function<Artifact, Consumer<TransformationContext>> transformations)
            throws IOException {
        this.output = requireNonNull(output, "output");
        this.pom = requireNonNull(pom, "pom").toAbsolutePath();
        if (!Files.isRegularFile(pom)) {
            Files.createDirectories(pom.getParent());
            Files.writeString(pom, blankPomSupplier.get(), StandardCharsets.UTF_8);
        }
        this.artifactMatcher = requireNonNull(artifactMatcher, "artifactMatcher");
        this.artifactMapper = requireNonNull(artifactMapper, "artifactMapper");
        this.transformations = requireNonNull(transformations, "transformations");

        this.applicableTransformations = new ArrayList<>();
    }

    public Path getPomPath() {
        return pom;
    }

    @Override
    public void accept(Artifact artifact) throws IOException {
        requireNonNull(artifact, "artifact");
        if (artifactMatcher.test(artifact)) {
            Consumer<TransformationContext> transformation = transformations.apply(artifactMapper.apply(artifact));
            if (transformation != null) {
                output.chatter("Accepted {}", artifact);
                applicableTransformations.add(transformation);
            }
        }
    }

    @Override
    public void cleanup(Exception e) {
        try {
            Files.deleteIfExists(pom);
        } catch (IOException ex) {
            // ignore
        }
    }

    @Override
    public void close() throws IOException {
        if (!applicableTransformations.isEmpty()) {
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

            TransformationContext context = () -> document;
            for (Consumer<TransformationContext> transformation : applicableTransformations) {
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

    private void normaliseLineEndings(Document document, String lineSeparator) {
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
