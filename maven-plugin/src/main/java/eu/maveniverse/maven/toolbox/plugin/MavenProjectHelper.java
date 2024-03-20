/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin;

import eu.maveniverse.maven.toolbox.shared.ResolutionRoot;
import java.util.stream.Collectors;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.artifact.DefaultArtifact;

public final class MavenProjectHelper {
    private MavenProjectHelper() {}

    public static ResolutionRoot toRoot(
            MavenProject mavenProject,
            ArtifactHandlerManager artifactHandlerManager,
            ArtifactTypeRegistry artifactTypeRegistry) {
        return ResolutionRoot.ofNotLoaded(new DefaultArtifact(
                        mavenProject.getGroupId(),
                        mavenProject.getArtifactId(),
                        artifactHandlerManager
                                .getArtifactHandler(mavenProject.getPackaging())
                                .getExtension(),
                        mavenProject.getVersion()))
                .withDependencies(mavenProject.getDependencies().stream()
                        .map(d -> RepositoryUtils.toDependency(d, artifactTypeRegistry))
                        .collect(Collectors.toList()))
                .withManagedDependencies(mavenProject.getDependencyManagement().getDependencies().stream()
                        .map(d -> RepositoryUtils.toDependency(d, artifactTypeRegistry))
                        .collect(Collectors.toList()))
                .build();
    }
}
