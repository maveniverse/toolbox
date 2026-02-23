/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static java.util.Objects.requireNonNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;

/**
 * A dependency visitor that compares to trees. Reuses same infra as {@link DependencyGraphDumper}.
 */
public class DependencyGraphComparator implements DependencyVisitor {
    private final Consumer<String> consumer;
    private final List<Function<DependencyNode, String>> decorators;
    private final DependencyGraphDumper.LineFormatter lineFormatter;

    private Deque<DependencyNode> nodes;
    private int cmp;

    /**
     * Creates instance with given consumer.
     *
     * @param consumer The string consumer, must not be {@code null}.
     */
    public DependencyGraphComparator(Consumer<String> consumer) {
        this(consumer, DependencyGraphDumper.defaultsWith());
    }

    /**
     * Creates instance with given consumer and decorators.
     *
     * @param consumer The string consumer, must not be {@code null}.
     * @param decorators The decorators to apply, must not be {@code null}.
     */
    public DependencyGraphComparator(
            Consumer<String> consumer, Collection<Function<DependencyNode, String>> decorators) {
        this(consumer, decorators, new DependencyGraphDumper.LineFormatter());
    }

    /**
     * Creates instance with given consumer and decorators.
     *
     * @param consumer The string consumer, must not be {@code null}.
     * @param decorators The decorators to apply, must not be {@code null}.
     * @param lineFormatter The {@link DependencyGraphDumper.LineFormatter}, must not be {@code null}.
     */
    public DependencyGraphComparator(
            Consumer<String> consumer,
            Collection<Function<DependencyNode, String>> decorators,
            DependencyGraphDumper.LineFormatter lineFormatter) {
        this.consumer = requireNonNull(consumer);
        this.decorators = new ArrayList<>(decorators);
        this.lineFormatter = requireNonNull(lineFormatter);
    }

    public void compare(DependencyNode root1, DependencyNode root2) {
        final Deque<ArrayDeque<DependencyNode>> ns1 = new ArrayDeque<>();
        final Deque<ArrayDeque<DependencyNode>> ns2 = new ArrayDeque<>();
        ns1.push(new ArrayDeque<>(List.of(root1)));
        ns2.push(new ArrayDeque<>(List.of(root2)));
        while (!ns1.isEmpty() || !ns2.isEmpty()) {
            if (ns1.isEmpty()) {
                cmp = 1;
                while (!ns2.isEmpty()) {
                    ArrayDeque<DependencyNode> path = ns2.pop();
                    DependencyNode current = path.pop();
                    this.nodes = path;
                    current.accept(this);
                }
                return;
            } else if (ns2.isEmpty()) {
                cmp = -1;
                while (!ns1.isEmpty()) {
                    ArrayDeque<DependencyNode> path = ns1.pop();
                    DependencyNode current = path.pop();
                    this.nodes = path;
                    current.accept(this);
                }
                return;
            }

            ArrayDeque<DependencyNode> path1 = ns1.pop();
            ArrayDeque<DependencyNode> path2 = ns2.pop();
            DependencyNode dn1 = path1.peek();
            DependencyNode dn2 = path2.peek();

            if (Objects.equals(dn1.getArtifact(), dn2.getArtifact())) {
                consumer.accept(lineFormatter.formatLine(0, path1, decorators));
            } else {
                consumer.accept(lineFormatter.formatLine(-1, path1, decorators));
                consumer.accept(lineFormatter.formatLine(1, path2, decorators));
            }

            List<DependencyNode> children1 = new ArrayList<>(dn1.getChildren());
            Collections.reverse(children1);
            for (DependencyNode child : children1) {
                ArrayDeque<DependencyNode> path = new ArrayDeque<>(path1);
                path.push(child);
                ns1.push(path);
            }
            List<DependencyNode> children2 = new ArrayList<>(dn2.getChildren());
            Collections.reverse(children2);
            for (DependencyNode child : children2) {
                ArrayDeque<DependencyNode> path = new ArrayDeque<>(path2);
                path.push(child);
                ns2.push(path);
            }
        }
    }

    @Override
    public boolean visitEnter(DependencyNode node) {
        nodes.push(node);
        consumer.accept(lineFormatter.formatLine(cmp, nodes, decorators));
        return true;
    }

    @Override
    public boolean visitLeave(DependencyNode node) {
        if (!nodes.isEmpty()) {
            nodes.pop();
        }
        return true;
    }
}
