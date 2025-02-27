/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin;

import eu.maveniverse.maven.toolbox.shared.DependencyMatcher;
import eu.maveniverse.maven.toolbox.shared.ReactorLocator;
import eu.maveniverse.maven.toolbox.shared.ResolutionRoot;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;

/**
 * Support class for "project aware" Mojos.
 */
public abstract class MPMojoSupport extends MojoSupport {
    @Component
    protected MavenSession mavenSession;

    @Component
    protected MavenProject mavenProject;

    @Component
    protected ArtifactHandlerManager artifactHandlerManager;

    /**
     * The repository vendor to use for Search RR backend ("central", "nx2" or any other extractor). If empty,
     * heuristics will be applied to figure out.
     */
    @Parameter(property = "toolbox.search.backend.type")
    private String repositoryVendor;

    protected String getRepositoryVendor() {
        if (repositoryVendor != null) {
            return repositoryVendor;
        }
        return mavenProject.getProperties().getProperty("toolbox.search.backend.type");
    }

    protected List<Dependency> toDependencies(List<org.apache.maven.model.Dependency> dependencies) {
        ArtifactTypeRegistry artifactTypeRegistry =
                mavenSession.getRepositorySession().getArtifactTypeRegistry();
        return dependencies.stream()
                .map(d -> RepositoryUtils.toDependency(d, artifactTypeRegistry))
                .collect(Collectors.toList());
    }

    protected ResolutionRoot projectAsResolutionRoot() {
        ResolutionRoot.Builder builder = ResolutionRoot.ofNotLoaded(new DefaultArtifact(
                        mavenProject.getGroupId(),
                        mavenProject.getArtifactId(),
                        artifactHandlerManager
                                .getArtifactHandler(mavenProject.getPackaging())
                                .getExtension(),
                        mavenProject.getVersion()))
                .withDependencies(toDependencies(mavenProject.getDependencies()));
        if (mavenProject.getDependencyManagement() != null) {
            builder.withManagedDependencies(
                    toDependencies(mavenProject.getDependencyManagement().getDependencies()));
        }
        return builder.build();
    }

    protected List<ResolutionRoot> projectManagedDependenciesAsResolutionRoots(DependencyMatcher dependencyMatcher) {
        ResolutionRoot project = projectAsResolutionRoot();
        return project.getManagedDependencies().stream()
                .filter(d -> !isReactorDependency(d))
                .filter(dependencyMatcher)
                .map(d -> ResolutionRoot.ofLoaded(getToolboxCommando().toArtifact(d))
                        .build())
                .collect(Collectors.toList());
    }

    protected List<ResolutionRoot> projectDependenciesAsResolutionRoots(DependencyMatcher dependencyMatcher) {
        ResolutionRoot project = projectAsResolutionRoot();
        return project.getDependencies().stream()
                .filter(d -> !isReactorDependency(d))
                .filter(dependencyMatcher)
                .map(d -> ResolutionRoot.ofLoaded(getToolboxCommando().toArtifact(d))
                        .withManagedDependencies(project.getManagedDependencies())
                        .build())
                .collect(Collectors.toList());
    }

    protected boolean isReactorDependency(Dependency dependency) {
        return mavenSession.getAllProjects().stream()
                .anyMatch(p -> Objects.equals(
                                p.getGroupId(), dependency.getArtifact().getGroupId())
                        && Objects.equals(
                                p.getArtifactId(), dependency.getArtifact().getArtifactId())
                        && Objects.equals(
                                p.getVersion(), dependency.getArtifact().getVersion()));
    }

    protected ReactorLocator getReactorLocator(String selector) {
        return new MavenReactorLocator(mavenSession, selector);
    }
}
