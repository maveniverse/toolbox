/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

import java.util.Set;

/**
 * Build scope is certain combination of {@link ProjectPath} and {@link BuildPath}.
 */
public interface BuildScope {
    /**
     * The label.
     */
    String getId();

    /**
     * The project paths this scope belongs to.
     */
    Set<ProjectPath> getProjectPaths();

    /**
     * The build paths this scope belongs to.
     */
    Set<BuildPath> getBuildPaths();
}
