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
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;

/**
 * Construction to represent "projects" abstraction.
 */
public interface ProjectLocator extends Artifacts.Source {
    /**
     * Represents single project.
     */
    interface Project {
        Artifact getArtifact();

        Optional<Artifact> getParent();

        List<Dependency> getDependencies();
    }

    /**
     * Returns "root" project, never {@code null}.
     */
    Project getRootProject();

    /**
     * Returns "current" project, never {@code null}.
     */
    Project getCurrentProject();

    /**
     * Returns list of all projects, never {@code null}.
     */
    List<Project> getAllProjects();

    /**
     * Locates project by given artifact. If not present, it means artifact is "external" relative to these projects.
     */
    Optional<Project> locateProject(Artifact artifact);

    /**
     * Locates children projects of given project.
     */
    List<Project> locateChildren(Project project);
}
