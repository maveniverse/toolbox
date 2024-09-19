/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

import eu.maveniverse.maven.toolbox.shared.internal.Artifacts;
import java.util.List;

/**
 * Construction to represent "reactor" abstraction.
 */
public interface ReactorLocator extends ProjectLocator, Artifacts.Source {
    /**
     * Returns "top level" project, never {@code null}.
     */
    Project getTopLevelProject();

    /**
     * Returns "current" project, never {@code null}.
     */
    Project getCurrentProject();

    /**
     * Returns list of all projects, never {@code null}.
     */
    List<Project> getAllProjects();

    /**
     * Locates children projects of given project within reactor, that is projects that refer to passed in project
     * as parent.
     */
    List<Project> locateChildren(Project project);

    /**
     * Locates children projects of given project within reactor, that is projects that are referred from passed in
     * project as modules.
     */
    List<Project> locateCollected(Project project);
}
