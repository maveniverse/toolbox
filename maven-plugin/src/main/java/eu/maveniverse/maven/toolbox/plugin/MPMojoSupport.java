/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin;

import eu.maveniverse.maven.toolbox.shared.ResolutionRoot;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;

/**
 * Support class for "project aware" Mojos.
 */
public abstract class MPMojoSupport extends MojoSupport {
    @Parameter(defaultValue = "${settings}", readonly = true, required = true)
    protected Settings settings;

    @Component
    protected MavenProject mavenProject;

    @Component
    protected MavenSession mavenSession;

    @Component
    protected ArtifactHandlerManager artifactHandlerManager;

    protected List<Dependency> toDependencies(List<org.apache.maven.model.Dependency> dependencies) {
        ArtifactTypeRegistry artifactTypeRegistry =
                mavenSession.getRepositorySession().getArtifactTypeRegistry();
        return dependencies.stream()
                .map(d -> RepositoryUtils.toDependency(d, artifactTypeRegistry))
                .collect(Collectors.toList());
    }

    protected ResolutionRoot projectAsResolutionRoot() {
        return ResolutionRoot.ofNotLoaded(new DefaultArtifact(
                        mavenProject.getGroupId(),
                        mavenProject.getArtifactId(),
                        artifactHandlerManager
                                .getArtifactHandler(mavenProject.getPackaging())
                                .getExtension(),
                        mavenProject.getVersion()))
                .withDependencies(toDependencies(mavenProject.getDependencies()))
                .withManagedDependencies(
                        toDependencies(mavenProject.getDependencyManagement().getDependencies()))
                .build();
    }
}
