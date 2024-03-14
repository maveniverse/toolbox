/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import eu.maveniverse.maven.toolbox.shared.BuildPath;
import eu.maveniverse.maven.toolbox.shared.DependencyScope;
import eu.maveniverse.maven.toolbox.shared.ProjectPath;
import eu.maveniverse.maven.toolbox.shared.ResolutionScope;
import eu.maveniverse.maven.toolbox.shared.ScopeManager;
import java.util.Optional;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyGraphTransformer;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.DependencyFilter;

/**
 * Internal scope manager.
 */
public interface InternalScopeManager extends ScopeManager {
    /**
     * The "width" of scope: is basically sum of all distinct {@link ProjectPath} and {@link BuildPath} that are
     * in build scopes the scope is present in. The more of them, the "wider" is the scope. Transitive scopes are
     * weighted more as well.
     * <p>
     * The {@link ProjectPath#order()} makes given path "weigh" more. So a scope being present only in
     * "main" project path is wider than scope being present only in "test" project path.
     * <p>
     * Interpretation: the bigger the returned integer is, the "wider" the scope is. The numbers should not serve
     * any other purposes, merely to sort scope instances by "width" (i.e. from "widest" to "narrowest").
     */
    int getDependencyScopeWidth(DependencyScope dependencyScope);

    /**
     * Returns the {@link BuildScope} that this scope deem as main.
     */
    Optional<BuildScope> getDependencyScopeMainProjectBuildScope(DependencyScope dependencyScope);

    /**
     * Resolver specific: dependency selector to be used to support this scope (with its dependency
     * and resolution scopes).
     */
    DependencySelector getDependencySelector(ResolutionScope resolutionScope);

    /**
     * Resolver specific: dependency graph transformer to be used to support this scope (with its dependency
     * and resolution scopes).
     */
    DependencyGraphTransformer getDependencyGraphTransformer(ResolutionScope resolutionScope);

    /**
     * Resolver specific: post-processing to be used to support this scope (with its dependency
     * and resolution scopes).
     */
    CollectResult postProcess(ResolutionScope resolutionScope, CollectResult collectResult);

    /**
     * Resolver specific: dependency filter to be used to support this scope (with its dependency
     * and resolution scopes).
     */
    DependencyFilter getDependencyFilter(ResolutionScope resolutionScope);
}
