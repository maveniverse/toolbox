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
 * A dependency visitor that dumps the graph to any {@link Consumer}{@code <String>}. Meant for diagnostic and testing, as
 * it may output the graph to standard output, error or even some logging interface.
 * <p>
 * Copy of the corresponding class from Resolver, to retain same output across Maven 3.6+
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
        final Deque<DependencyNode> ns1 = new ArrayDeque<>();
        final Deque<DependencyNode> ns2 = new ArrayDeque<>();
        final Deque<DependencyNode> processed1 = new ArrayDeque<>();
        final Deque<DependencyNode> processed2 = new ArrayDeque<>();
        ns1.push(root1);
        ns2.push(root2);
        while (!ns1.isEmpty() || !ns2.isEmpty()) {
            if (ns1.isEmpty()) {
                cmp = 1;
                this.nodes = processed2;
                ns2.peek().accept(this);
                return;
            } else if (ns2.isEmpty()) {
                cmp = -1;
                this.nodes = processed1;
                ns1.peek().accept(this);
                return;
            }

            DependencyNode dn1 = ns1.pop();
            DependencyNode dn2 = ns2.pop();

            processed1.push(dn1);
            processed2.push(dn2);

            if (Objects.equals(dn1.getArtifact(), dn2.getArtifact())) {
                consumer.accept(lineFormatter.formatLine(0, processed1, decorators));
            } else {
                consumer.accept(lineFormatter.formatLine(-1, processed1, decorators));
                consumer.accept(lineFormatter.formatLine(1, processed2, decorators));
            }

            if (dn1.getChildren().isEmpty()) {
                while (dn1.getChildren().isEmpty() && !processed1.isEmpty()) {
                    dn1 = processed1.pop();
                }
            } else {
                List<DependencyNode> children = new ArrayList<>(dn1.getChildren());
                Collections.reverse(children);
                children.forEach(ns1::push);
            }
            if (dn2.getChildren().isEmpty()) {
                while (dn2.getChildren().isEmpty() && !processed2.isEmpty()) {
                    dn2 = processed2.pop();
                }
            } else {
                List<DependencyNode> children = new ArrayList<>(dn2.getChildren());
                Collections.reverse(children);
                children.forEach(ns2::push);
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
