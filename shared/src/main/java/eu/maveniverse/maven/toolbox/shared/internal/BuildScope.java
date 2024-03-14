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

    /**
     * Returns the "order" of this scope, usable to sort against other instances.
     * Expected natural order is "main-compile", "test-compile"... (basically like the processing order).
     * <p>
     * Note: this order is unrelated to {@link ProjectPath#order()} and {@link BuildPath#order()} and
     * should be used only to sort build scope instances.
     */
    int order();
}
