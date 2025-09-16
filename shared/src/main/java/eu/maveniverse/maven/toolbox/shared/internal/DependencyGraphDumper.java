/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static java.util.Objects.requireNonNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.repository.ArtifactRepository;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.eclipse.aether.version.VersionConstraint;

/**
 * A dependency visitor that dumps the graph to any {@link Consumer}{@code <String>}. Meant for diagnostic and testing, as
 * it may output the graph to standard output, error or even some logging interface.
 * <p>
 * Copy of the corresponding class from Resolver, to retain same output across Maven 3.6+
 */
public class DependencyGraphDumper implements DependencyVisitor {
    public static class LineFormatter {
        /**
         * Formats line with markers from where it comes using {@code cmp}: if 0, both have it, if less than 0 then
         * left have it, if greater than 0 then right have it.
         */
        public String formatLine(
                int cmp, Deque<DependencyNode> nodes, List<Function<DependencyNode, String>> decorators) {
            return (cmp == 0 ? "    " : (cmp < 0 ? "--- " : "+++ ")) + formatLine(nodes, decorators);
        }

        /**
         * Formats <em>one line</em> out of at least 2 segments: indentation, main label, and zero or more optional labels.
         */
        public String formatLine(Deque<DependencyNode> nodes, List<Function<DependencyNode, String>> decorators) {
            return formatIndentation(nodes) + String.join(" ", formatNode(nodes, decorators));
        }

        protected String formatIndentation(Deque<DependencyNode> nodes) {
            return formatIndentation(nodes, "\\- ", "+- ", "   ", "|  ");
        }

        protected String formatIndentation(
                Deque<DependencyNode> nodes, String endLastStr, String endStr, String midLastStr, String midStr) {
            StringBuilder buffer = new StringBuilder(128);
            Iterator<DependencyNode> iter = nodes.descendingIterator();
            DependencyNode parent = iter.hasNext() ? iter.next() : null;
            DependencyNode child = iter.hasNext() ? iter.next() : null;
            while (parent != null && child != null) {
                boolean lastChild =
                        parent.getChildren().get(parent.getChildren().size() - 1) == child;
                boolean end = child == nodes.peekFirst();
                String indent;
                if (end) {
                    indent = lastChild ? endLastStr : endStr;
                } else {
                    indent = lastChild ? midLastStr : midStr;
                }
                buffer.append(indent);
                parent = child;
                child = iter.hasNext() ? iter.next() : null;
            }
            return buffer.toString();
        }

        /**
         * Formats node out of two segments: main label and zero or more optional labels.
         */
        protected List<String> formatNode(
                Deque<DependencyNode> nodes, List<Function<DependencyNode, String>> decorators) {
            DependencyNode node = requireNonNull(nodes.peek(), "bug: should not happen");
            StringBuilder buffer = new StringBuilder(128);
            for (Function<DependencyNode, String> decorator : decorators) {
                String decoration = decorator.apply(node);
                if (decoration != null) {
                    if (buffer.isEmpty()) {
                        buffer.append(decoration);
                    } else {
                        buffer.append(" ").append(decoration);
                    }
                }
            }
            return Arrays.asList(ArtifactIdUtils.toId(node.getArtifact()), buffer.toString());
        }
    }

    /**
     * Decorator of "effective dependency": shows effective scope and optionality.
     */
    public static Function<DependencyNode, String> effectiveDependency() {
        return dependencyNode -> {
            Dependency d = dependencyNode.getDependency();
            if (d != null) {
                if (!d.getScope().isEmpty()) {
                    String result = d.getScope();
                    if (d.isOptional()) {
                        result += ", optional";
                    }
                    return "[" + result + "]";
                }
            }
            return null;
        };
    }

    /**
     * Helper for premanaged state handling.
     */
    public static Function<DependencyNode, Boolean> isPremanaged() {
        return node -> (DependencyManagerUtils.getPremanagedVersion(node) != null
                        && !Objects.equals(
                                DependencyManagerUtils.getPremanagedVersion(node),
                                node.getDependency().getArtifact().getVersion()))
                || (DependencyManagerUtils.getPremanagedScope(node) != null
                        && !Objects.equals(
                                DependencyManagerUtils.getPremanagedScope(node),
                                node.getDependency().getScope()))
                || (DependencyManagerUtils.getPremanagedOptional(node) != null
                        && !Objects.equals(
                                DependencyManagerUtils.getPremanagedOptional(node),
                                node.getDependency().getOptional()))
                || (DependencyManagerUtils.getPremanagedExclusions(node) != null
                        && !Objects.equals(
                                DependencyManagerUtils.getPremanagedExclusions(node),
                                node.getDependency().getExclusions()));
    }
    /**
     * Decorator of "managed version": explains on nodes what was managed.
     */
    public static Function<DependencyNode, String> premanagedVersion() {
        return dependencyNode -> {
            if (dependencyNode.getArtifact() != null) {
                String premanagedVersion = DependencyManagerUtils.getPremanagedVersion(dependencyNode);
                if (premanagedVersion != null
                        && !premanagedVersion.equals(
                                dependencyNode.getArtifact().getBaseVersion())) {
                    return "(version managed from " + premanagedVersion + ")";
                }
            }
            return null;
        };
    }
    /**
     * Decorator of "managed scope": explains on nodes what was managed.
     */
    public static Function<DependencyNode, String> premanagedScope() {
        return dependencyNode -> {
            Dependency d = dependencyNode.getDependency();
            if (d != null) {
                String premanagedScope = DependencyManagerUtils.getPremanagedScope(dependencyNode);
                if (premanagedScope != null && !premanagedScope.equals(d.getScope())) {
                    return "(scope managed from " + premanagedScope + ")";
                }
            }
            return null;
        };
    }
    /**
     * Decorator of "managed optionality": explains on nodes what was managed.
     */
    public static Function<DependencyNode, String> premanagedOptional() {
        return dependencyNode -> {
            Dependency d = dependencyNode.getDependency();
            if (d != null) {
                Boolean premanagedOptional = DependencyManagerUtils.getPremanagedOptional(dependencyNode);
                if (premanagedOptional != null && !premanagedOptional.equals(d.getOptional())) {
                    return "(optionality managed from " + premanagedOptional + ")";
                }
            }
            return null;
        };
    }
    /**
     * Decorator of "managed exclusions": explains on nodes what was managed.
     */
    public static Function<DependencyNode, String> premanagedExclusions() {
        return dependencyNode -> {
            Dependency d = dependencyNode.getDependency();
            if (d != null) {
                Collection<Exclusion> premanagedExclusions =
                        DependencyManagerUtils.getPremanagedExclusions(dependencyNode);
                if (premanagedExclusions != null) {
                    if (!equals(premanagedExclusions, d.getExclusions())) {
                        return "(exclusions managed from " + premanagedExclusions + ")";
                    } else {
                        return "(exclusions applied: " + premanagedExclusions.size() + ")";
                    }
                }
            }
            return null;
        };
    }
    /**
     * Decorator of "managed properties": explains on nodes what was managed.
     */
    public static Function<DependencyNode, String> premanagedProperties() {
        return dependencyNode -> {
            if (dependencyNode.getArtifact() != null) {
                Map<String, String> premanagedProperties =
                        DependencyManagerUtils.getPremanagedProperties(dependencyNode);
                if (premanagedProperties != null
                        && !equals(
                                premanagedProperties,
                                dependencyNode.getArtifact().getProperties())) {
                    return "(properties managed from " + premanagedProperties + ")";
                }
            }
            return null;
        };
    }
    /**
     * Decorator of "range member": explains on nodes what range it participates in.
     */
    public static Function<DependencyNode, String> rangeMember() {
        return dependencyNode -> {
            VersionConstraint constraint = dependencyNode.getVersionConstraint();
            if (constraint != null && constraint.getRange() != null) {
                return "(range '" + constraint.getRange() + "')";
            }
            return null;
        };
    }
    /**
     * Decorator of "winner node": explains on losers why lost.
     */
    public static Function<DependencyNode, String> winnerNode() {
        return dependencyNode -> {
            if (dependencyNode.getArtifact() != null) {
                DependencyNode winner =
                        (DependencyNode) dependencyNode.getData().get(ConflictResolver.NODE_DATA_WINNER);
                if (winner != null) {
                    if (ArtifactIdUtils.equalsId(dependencyNode.getArtifact(), winner.getArtifact())) {
                        return "(nearer exists)";
                    } else {
                        Artifact w = winner.getArtifact();
                        String result = "conflicts with ";
                        if (ArtifactIdUtils.toVersionlessId(dependencyNode.getArtifact())
                                .equals(ArtifactIdUtils.toVersionlessId(w))) {
                            result += w.getVersion();
                        } else {
                            result += w;
                        }
                        return "(" + result + ")";
                    }
                }
            }
            return null;
        };
    }
    /**
     * Decorator of "origin": prints out ids from {@link DependencyNode#getRepositories()}.
     */
    public static Function<DependencyNode, String> origin() {
        return dependencyNode -> {
            if (dependencyNode.getRepositories().isEmpty()) {
                return "";
            }
            return "(origin: "
                    + dependencyNode.getRepositories().stream()
                            .map(ArtifactRepository::getId)
                            .collect(Collectors.joining(","))
                    + ")";
        };
    }
    /**
     * Decorator of "artifact properties": prints out asked properties, if present.
     */
    public static Function<DependencyNode, String> artifactProperties(Collection<String> properties) {
        requireNonNull(properties, "properties");
        return dependencyNode -> {
            if (!properties.isEmpty() && dependencyNode.getDependency() != null) {
                String props = properties.stream()
                        .map(p -> p + "="
                                + dependencyNode.getDependency().getArtifact().getProperty(p, "n/a"))
                        .collect(Collectors.joining(","));
                if (!props.isEmpty()) {
                    return "(" + props + ")";
                }
            }
            return null;
        };
    }

    /**
     * The standard "default" decorators.
     *
     * @since 2.0.0
     */
    private static final List<Function<DependencyNode, String>> DEFAULT_DECORATORS =
            Collections.unmodifiableList(Arrays.asList(
                    effectiveDependency(),
                    premanagedVersion(),
                    premanagedScope(),
                    premanagedOptional(),
                    premanagedExclusions(),
                    premanagedProperties(),
                    rangeMember(),
                    winnerNode(),
                    origin()));

    /**
     * Extends {@link #DEFAULT_DECORATORS} decorators with passed in ones.
     */
    @SafeVarargs
    public static List<Function<DependencyNode, String>> defaultsWith(Function<DependencyNode, String>... extras) {
        return defaultsWith(Arrays.asList(extras));
    }

    /**
     * Extends {@link #DEFAULT_DECORATORS} decorators with passed in ones.
     */
    public static List<Function<DependencyNode, String>> defaultsWith(
            Collection<Function<DependencyNode, String>> extras) {
        requireNonNull(extras, "extras");
        ArrayList<Function<DependencyNode, String>> result = new ArrayList<>(DEFAULT_DECORATORS);
        result.addAll(extras);
        return result;
    }

    private final Consumer<String> consumer;

    private final List<Function<DependencyNode, String>> decorators;

    private final LineFormatter lineFormatter;

    private final Deque<DependencyNode> nodes = new ArrayDeque<>();

    /**
     * Creates instance with given consumer.
     *
     * @param consumer The string consumer, must not be {@code null}.
     */
    public DependencyGraphDumper(Consumer<String> consumer) {
        this(consumer, DEFAULT_DECORATORS);
    }

    /**
     * Creates instance with given consumer and decorators.
     *
     * @param consumer The string consumer, must not be {@code null}.
     * @param decorators The decorators to apply, must not be {@code null}.
     */
    public DependencyGraphDumper(Consumer<String> consumer, Collection<Function<DependencyNode, String>> decorators) {
        this(consumer, decorators, new LineFormatter());
    }

    /**
     * Creates instance with given consumer and decorators.
     *
     * @param consumer The string consumer, must not be {@code null}.
     * @param decorators The decorators to apply, must not be {@code null}.
     * @param lineFormatter The {@link LineFormatter}, must not be {@code null}.
     */
    public DependencyGraphDumper(
            Consumer<String> consumer,
            Collection<Function<DependencyNode, String>> decorators,
            LineFormatter lineFormatter) {
        this.consumer = requireNonNull(consumer);
        this.decorators = new ArrayList<>(decorators);
        this.lineFormatter = requireNonNull(lineFormatter);
    }

    @Override
    public boolean visitEnter(DependencyNode node) {
        nodes.push(node);
        consumer.accept(lineFormatter.formatLine(nodes, decorators));
        return true;
    }

    @Override
    public boolean visitLeave(DependencyNode node) {
        if (!nodes.isEmpty()) {
            nodes.pop();
        }
        return true;
    }

    private static boolean equals(Collection<Exclusion> c1, Collection<Exclusion> c2) {
        return c1 != null && c2 != null && c1.size() == c2.size() && c1.containsAll(c2);
    }

    private static boolean equals(Map<String, String> m1, Map<String, String> m2) {
        return m1 != null
                && m2 != null
                && m1.size() == m2.size()
                && m1.entrySet().stream().allMatch(entry -> Objects.equals(m2.get(entry.getKey()), entry.getValue()));
    }
}
