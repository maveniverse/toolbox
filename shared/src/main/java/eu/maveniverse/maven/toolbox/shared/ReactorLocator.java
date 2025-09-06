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
import java.util.Optional;
import org.apache.maven.model.Model;
import org.eclipse.aether.artifact.Artifact;

/**
 * Construction to represent "reactor" abstraction.
 */
public interface ReactorLocator extends ProjectLocator, Artifacts.Source {
    interface ReactorProject extends Project {
        Model effectiveModel();
    }

    /**
     * Returns "top level" project, never {@code null}.
     */
    ReactorProject getTopLevelProject();

    /**
     * Returns the "selected" project, or empty optional.
     */
    Optional<ReactorProject> getSelectedProject();

    /**
     * Returns "current" project, never {@code null}.
     */
    ReactorProject getCurrentProject();

    /**
     * Returns selected (if any) or current project.
     */
    default ReactorProject getSelectedOrCurrentProject() {
        return getSelectedProject().orElse(getCurrentProject());
    }

    /**
     * Returns list of all projects, never {@code null}.
     */
    List<ReactorProject> getAllProjects();

    /**
     * Locates children projects of given project within reactor, that is projects that refer to passed in project
     * as parent.
     */
    List<ReactorProject> locateChildren(Project project);

    /**
     * Locates children projects of given project within reactor, that is projects that are referred from passed in
     * project as modules.
     */
    List<ReactorProject> locateCollected(Project project);

    @Override
    Optional<ReactorProject> locateProject(Artifact artifact);
}
