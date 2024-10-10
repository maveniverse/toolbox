/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.util.graph.selector.StaticDependencySelector;

public class LevelDependencySelector implements DependencySelector {
    private final int maxLevel;
    private final int currentLevel;

    public LevelDependencySelector(int maxLevel) {
        if (maxLevel < 1) {
            throw new IllegalArgumentException("maxLevel must be greater than 0");
        }
        this.maxLevel = maxLevel;
        this.currentLevel = 0;
    }

    private LevelDependencySelector(int maxLevel, int currentLevel) {
        this.maxLevel = maxLevel;
        this.currentLevel = currentLevel;
    }

    @Override
    public boolean selectDependency(Dependency dependency) {
        return true;
    }

    @Override
    public DependencySelector deriveChildSelector(DependencyCollectionContext context) {
        int newLevel = currentLevel + 1;
        if (newLevel > maxLevel) {
            return new StaticDependencySelector(false);
        } else {
            return new LevelDependencySelector(maxLevel, newLevel);
        }
    }
}
