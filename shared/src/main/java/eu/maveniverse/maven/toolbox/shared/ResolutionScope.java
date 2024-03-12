/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

import java.util.Set;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyGraphTransformer;
import org.eclipse.aether.collection.DependencySelector;

/**
 * Generic resolution scope.
 */
public interface ResolutionScope {
    /**
     * The mode of resolution scope: eliminate (remove all occurrences) or just remove.
     */
    enum Mode {
        /**
         * Mode where artifacts in non-wanted scopes are completely eliminated. In other words, this mode ensures
         * that if a dependency was removed due unwanted scope, it is guaranteed that no such dependency will appear
         * anywhere else in the resulting graph either.
         */
        ELIMINATE,

        /**
         * Mode where artifacts in non-wanted scopes are removed only. In other words, they will NOT prevent (as in
         * they will not "dominate") other possibly appearing occurrences of same artifact in the graph.
         */
        REMOVE
    }

    /**
     * The label.
     */
    String getId();

    /**
     * The language this scope belongs to.
     */
    Language getLanguage();

    /**
     * The operation mode of this scope.
     */
    Mode getMode();

    /**
     * The wanted set of presences this scope wants.
     */
    Set<BuildScope> getWantedPresence();

    /**
     * Resolver specific: dependency selector to be used to support this scope (with its dependency
     * and resolution scopes).
     */
    DependencySelector getDependencySelector();

    /**
     * Resolver specific: dependency graph transformer to be used to support this scope (with its dependency
     * and resolution scopes).
     */
    DependencyGraphTransformer getDependencyGraphTransformer();

    /**
     * Resolver specific: post-processing to be used to support this scope (with its dependency
     * and resolution scopes).
     */
    CollectResult postProcess(CollectResult collectResult);
}
