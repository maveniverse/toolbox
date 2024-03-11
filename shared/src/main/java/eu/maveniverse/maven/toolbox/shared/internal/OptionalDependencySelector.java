/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static java.util.Objects.requireNonNull;

import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.Dependency;

/**
 * A dependency selector that excludes optional dependencies which occur beyond given level.
 *
 * @see Dependency#isOptional()
 */
public final class OptionalDependencySelector implements DependencySelector {
    /**
     * Excludes optional dependencies always (from root).
     */
    public static OptionalDependencySelector fromRoot() {
        return from(1);
    }

    /**
     * Excludes optional transitive dependencies of direct dependencies.
     */
    public static OptionalDependencySelector fromDirect() {
        return from(2);
    }

    /**
     * Excludes optional transitive dependencies from given depth (1=root, 2=direct, 3=transitives of direct ones...).
     */
    public static OptionalDependencySelector from(int applyFrom) {
        if (applyFrom < 1) {
            throw new IllegalArgumentException("applyFrom must be non-zero and positive");
        }
        return new OptionalDependencySelector(0, applyFrom);
    }

    private final int depth;

    private final int applyFrom;

    private OptionalDependencySelector(int depth, int applyFrom) {
        this.depth = depth;
        this.applyFrom = applyFrom;
    }

    @Override
    public boolean selectDependency(Dependency dependency) {
        requireNonNull(dependency, "dependency cannot be null");
        return depth < applyFrom || !dependency.isOptional();
    }

    @Override
    public DependencySelector deriveChildSelector(DependencyCollectionContext context) {
        requireNonNull(context, "context cannot be null");
        return new OptionalDependencySelector(depth + 1, applyFrom);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (null == obj || !getClass().equals(obj.getClass())) {
            return false;
        }

        OptionalDependencySelector that = (OptionalDependencySelector) obj;
        return depth == that.depth && applyFrom == that.applyFrom;
    }

    @Override
    public int hashCode() {
        int hash = getClass().hashCode();
        hash = hash * 31 + depth;
        hash = hash * 31 + applyFrom;
        return hash;
    }

    @Override
    public String toString() {
        return String.format("%s(applies: %s)", this.getClass().getSimpleName(), depth >= applyFrom);
    }
}
