/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import eu.maveniverse.maven.toolbox.shared.BuildPath;
import eu.maveniverse.maven.toolbox.shared.ProjectPath;
import java.util.Collection;

/**
 * Build scope source.
 */
public interface BuildScopeSource {
    /**
     * Returns all project paths this source has.
     */
    Collection<ProjectPath> allProjectPaths();

    /**
     * Returns all build paths this source has.
     */
    Collection<BuildPath> allBuildPaths();

    /**
     * Processes all queries and returns all build scopes that query selected.
     */
    Collection<BuildScope> query(Collection<BuildScopeQuery> queries);
}
