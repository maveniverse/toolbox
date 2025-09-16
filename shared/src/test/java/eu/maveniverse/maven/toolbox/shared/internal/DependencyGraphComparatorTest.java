/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import java.util.List;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.junit.jupiter.api.Test;

public class DependencyGraphComparatorTest {
    @Test
    void simpleEquals() {
        DependencyGraphComparator comparator = new DependencyGraphComparator(System.out::println);
        DependencyNode r1 = new DefaultDependencyNode(new Dependency(new DefaultArtifact("group:artifact:1.0"), ""));
        r1.setChildren(List.of(
                new DefaultDependencyNode(new Dependency(new DefaultArtifact("group:a:1.0"), "compile")),
                new DefaultDependencyNode(new Dependency(new DefaultArtifact("group:b:1.0"), "compile"))));
        DependencyNode r2 = new DefaultDependencyNode(new Dependency(new DefaultArtifact("group:artifact:1.0"), ""));
        r2.setChildren(List.of(
                new DefaultDependencyNode(new Dependency(new DefaultArtifact("group:a:1.0"), "compile")),
                new DefaultDependencyNode(new Dependency(new DefaultArtifact("group:b:1.0"), "compile"))));
        comparator.compare(r1, r2);
    }

    @Test
    void simpleDiff() {
        DependencyGraphComparator comparator = new DependencyGraphComparator(System.out::println);
        DependencyNode r1 = new DefaultDependencyNode(new Dependency(new DefaultArtifact("group:artifact:1.0"), ""));
        r1.setChildren(List.of(
                new DefaultDependencyNode(new Dependency(new DefaultArtifact("group:a:1.0"), "compile")),
                new DefaultDependencyNode(new Dependency(new DefaultArtifact("group:b:1.0"), "compile"))));
        DependencyNode r2 = new DefaultDependencyNode(new Dependency(new DefaultArtifact("group:artifact:1.1"), ""));
        r2.setChildren(List.of(
                new DefaultDependencyNode(new Dependency(new DefaultArtifact("group:a:1.0"), "compile")),
                new DefaultDependencyNode(new Dependency(new DefaultArtifact("group:b:1.1"), "compile"))));
        comparator.compare(r1, r2);
    }

    @Test
    void simpleUnbalanced() {
        DependencyGraphComparator comparator = new DependencyGraphComparator(System.out::println);
        DependencyNode r1 = new DefaultDependencyNode(new Dependency(new DefaultArtifact("group:artifact:1.0"), ""));
        r1.setChildren(
                List.of(new DefaultDependencyNode(new Dependency(new DefaultArtifact("group:a:1.0"), "compile"))));
        DependencyNode r2 = new DefaultDependencyNode(new Dependency(new DefaultArtifact("group:artifact:1.0"), ""));
        r2.setChildren(List.of(
                new DefaultDependencyNode(new Dependency(new DefaultArtifact("group:a:1.0"), "compile")),
                new DefaultDependencyNode(new Dependency(new DefaultArtifact("group:b:1.0"), "compile"))));
        comparator.compare(r1, r2);
    }
}
