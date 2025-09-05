/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

import eu.maveniverse.maven.toolbox.shared.internal.Dependencies;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;

/**
 * Construction to represent "projects" abstraction and ability to locate them.
 */
public interface ProjectLocator {
    /**
     * Represents single project.
     */
    interface Project extends Dependencies.Source {
        Artifact artifact();

        Optional<Artifact> getParent();

        List<Dependency> dependencies();

        ProjectLocator origin();

        @Override
        default Stream<Dependency> get() {
            return dependencies().stream();
        }
    }
    /**
     * Locates project by given artifact. If not present, it means artifact is "external" relative to these projects.
     */
    Optional<? extends Project> locateProject(Artifact artifact);
}
