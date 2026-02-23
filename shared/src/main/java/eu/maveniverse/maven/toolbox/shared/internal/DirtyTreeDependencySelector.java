/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;

/**
 * A special dependency selector, that selects all until unless delegate unselected is met, and then it selects only
 * one level below of their children.
 */
public class DirtyTreeDependencySelector implements DependencySelector {
    private final DependencySelector delegate;
    private final DependencySelector filter;
    private final int maxLevelPast;
    private final ConcurrentHashMap<String, Boolean> stoppers;

    public DirtyTreeDependencySelector(DependencySelector delegate, DependencySelector filter, int maxLevelPast) {
        this.delegate = requireNonNull(delegate);
        this.filter = requireNonNull(filter);
        if (maxLevelPast < 0) {
            throw new IllegalArgumentException("maxLevelPast must be greater than or equal to 0");
        }
        this.maxLevelPast = maxLevelPast;
        this.stoppers = new ConcurrentHashMap<>();
    }

    @Override
    public boolean selectDependency(Dependency dependency) {
        boolean selected = delegate.selectDependency(dependency);
        if (!selected) {
            stoppers.put(ArtifactIdUtils.toId(dependency.getArtifact()), Boolean.TRUE);
        }
        return filter.selectDependency(dependency);
    }

    @Override
    public DependencySelector deriveChildSelector(DependencyCollectionContext context) {
        if (context.getDependency() != null
                && stoppers.containsKey(
                        ArtifactIdUtils.toId(context.getDependency().getArtifact()))) {
            return new LevelDependencySelector(maxLevelPast);
        } else {
            return new DirtyTreeDependencySelector(
                    delegate.deriveChildSelector(context), filter.deriveChildSelector(context), maxLevelPast);
        }
    }
}
