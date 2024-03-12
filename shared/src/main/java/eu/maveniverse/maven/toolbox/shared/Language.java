/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

import java.util.Collection;
import java.util.Optional;

/**
 * Language definition.
 */
public interface Language {
    /**
     * The label.
     */
    String getId();

    /**
     * Description (meant for human consumption).
     */
    String getDescription();

    /**
     * Returns the "system" scope, if language has it.
     * <p>
     * This is a special scope. In this scope case, Resolver should handle it specially, as it has no POM (so is
     * always a leaf on graph), is not in any repository, but is actually hosted on host OS file system. On resolution
     * resolver merely checks is file present or not.
     */
    Optional<DependencyScope> getSystemScope();

    /**
     * Returns a language specific dependency scope by label.
     */
    Optional<DependencyScope> getDependencyScope(String id);

    /**
     * Returns the "universe" (all) of language specific dependency scopes.
     */
    Collection<DependencyScope> getDependencyScopeUniverse();

    /**
     * Returns a language specific resolution scope by label.
     */
    Optional<ResolutionScope> getResolutionScope(String id);

    /**
     * Returns the "universe" (all) of language specific resolution scopes.
     */
    Collection<ResolutionScope> getResolutionScopeUniverse();
}
