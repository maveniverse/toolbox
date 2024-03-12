/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

import java.util.Optional;
import java.util.Set;

/**
 * Generic dependency scope.
 */
public interface DependencyScope {
    /**
     * The label.
     */
    String getId();

    /**
     * The language this scope belongs to.
     */
    Language getLanguage();

    /**
     * Is it transitive scope?
     */
    boolean isTransitive();

    /**
     * The presence of this scope.
     */
    Set<BuildScope> getPresence();

    /**
     * The "width" of this scope: is basically sum of all distinct {@link ProjectPath} and {@link BuildPath} that are
     * in {@link #getPresence()}. The more of them, the "wider" is the scope. Transitive scopes are weighted more
     * as well.
     * <p>
     * The {@link ProjectPath#order()} makes given path "weigh" more. So a scope being present only in
     * "main" project path is wider than scope being present only in "test" project path.
     * <p>
     * Interpretation: the bigger the returned integer is, the "wider" the scope is. The numbers should not serve
     * any other purposes, merely to sort scope instances by "width" (i.e. from "widest" to "narrowest").
     */
    int width();

    /**
     * Derives this scope from parent scope. This is how "compile" dependencies are changed to "test" dependencies
     * when their parent are put in "test" scope.
     */
    Optional<DependencyScope> deriveFromParent(DependencyScope parent);

    /**
     * Returns the {@link BuildScope} that this scope deem as main.
     */
    Optional<BuildScope> getMainProjectBuildScope();
}
