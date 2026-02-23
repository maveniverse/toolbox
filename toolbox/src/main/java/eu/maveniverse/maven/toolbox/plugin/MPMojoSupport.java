/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin;

import eu.maveniverse.maven.toolbox.shared.DependencyMatcher;
import eu.maveniverse.maven.toolbox.shared.ReactorLocator;
import eu.maveniverse.maven.toolbox.shared.ResolutionRoot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.Interpolator;
import org.codehaus.plexus.interpolation.MapBasedValueSource;
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.util.artifact.JavaScopes;

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
        return projectAsResolutionRoot(true);
    }

    protected ResolutionRoot projectAsResolutionRoot(boolean effective) {
        ResolutionRoot.Builder builder = ResolutionRoot.ofNotLoaded(new DefaultArtifact(
                mavenProject.getGroupId(),
                mavenProject.getArtifactId(),
                artifactHandlerManager
                        .getArtifactHandler(mavenProject.getPackaging())
                        .getExtension(),
                mavenProject.getVersion()));
        if (effective) {
            builder.withDependencies(toDependencies(mavenProject.getDependencies()));
            if (mavenProject.getDependencyManagement() != null) {
                builder.withManagedDependencies(
                        toDependencies(mavenProject.getDependencyManagement().getDependencies()));
            }
        } else {
            List<Dependency> dependencies = new ArrayList<>();
            Function<String, String> projectInterpolator = projectInterpolator();
            for (org.apache.maven.model.Dependency dependency :
                    mavenProject.getOriginalModel().getDependencies()) {
                dependencies.add(new Dependency(
                        new DefaultArtifact(projectInterpolator.apply(mavenDependencyToGavString(dependency))),
                        dependency.getScope()));
            }
            // TODO: swap out entries with the ones from effective POM
            builder.withDependencies(dependencies);
            if (mavenProject.getOriginalModel().getDependencyManagement() != null) {
                List<Dependency> managedDependencies = new ArrayList<>();
                for (org.apache.maven.model.Dependency dependency : mavenProject
                        .getOriginalModel()
                        .getDependencyManagement()
                        .getDependencies()) {
                    managedDependencies.add(new Dependency(
                            new DefaultArtifact(projectInterpolator.apply(mavenDependencyToGavString(dependency))),
                            dependency.getScope()));
                }
                // TODO: swap out entries with the ones from effective POM; except for import/pom!
                builder.withManagedDependencies(managedDependencies);
            }
        }
        return builder.build();
    }

    private String mavenDependencyToGavString(org.apache.maven.model.Dependency dependency) {
        return dependency.getClassifier() == null || dependency.getClassifier().isBlank()
                ? dependency.getGroupId() + ":" + dependency.getArtifactId() + ":"
                        + artifactHandlerManager
                                .getArtifactHandler(dependency.getType())
                                .getExtension() + ":" + dependency.getVersion()
                : dependency.getGroupId() + ":" + dependency.getArtifactId() + ":"
                        + dependency.getClassifier() + ":"
                        + artifactHandlerManager
                                .getArtifactHandler(dependency.getType())
                                .getExtension() + ":" + dependency.getVersion();
    }

    protected List<ResolutionRoot> projectManagedDependenciesAsResolutionRoots(
            boolean effective, DependencyMatcher dependencyMatcher) {
        ResolutionRoot project = projectAsResolutionRoot(effective);
        return project.getManagedDependencies().stream()
                .filter(d -> !isReactorProject(d.getArtifact()))
                .filter(dependencyMatcher)
                .map(d -> ResolutionRoot.ofLoaded(getToolboxCommando().toArtifact(d))
                        .build())
                .collect(Collectors.toList());
    }

    protected List<ResolutionRoot> projectDependenciesAsResolutionRoots(DependencyMatcher dependencyMatcher) {
        ResolutionRoot project = projectAsResolutionRoot();
        return project.getDependencies().stream()
                .filter(d -> !isReactorProject(d.getArtifact()))
                .filter(dependencyMatcher)
                .map(d -> ResolutionRoot.ofLoaded(getToolboxCommando().toArtifact(d))
                        .withManagedDependencies(project.getManagedDependencies())
                        .applyManagedDependencies(true)
                        .cutDependencies((d.getOptional() != null && d.getOptional())
                                || JavaScopes.PROVIDED.equals(d.getScope()))
                        .build())
                .collect(Collectors.toList());
    }

    protected boolean isReactorProject(Artifact artifact) {
        return mavenSession.getAllProjects().stream()
                .anyMatch(p -> Objects.equals(p.getGroupId(), artifact.getGroupId())
                        && Objects.equals(p.getArtifactId(), artifact.getArtifactId())
                        && Objects.equals(p.getVersion(), artifact.getVersion()));
    }

    protected ReactorLocator getReactorLocator(String selector) {
        return new MavenReactorLocator(mavenSession, selector);
    }

    protected Function<String, String> projectInterpolator() {
        Interpolator interpolator = new StringSearchInterpolator();
        interpolator.addValueSource(new PropertiesBasedValueSource(mavenProject.getProperties()));
        HashMap<String, String> projectAttributes = new HashMap<>();
        projectAttributes.put("project.groupId", mavenProject.getGroupId());
        projectAttributes.put("project.artifactId", mavenProject.getArtifactId());
        projectAttributes.put("project.version", mavenProject.getVersion());
        interpolator.addValueSource(new MapBasedValueSource(projectAttributes));
        return s -> {
            try {
                return interpolator.interpolate(s);
            } catch (InterpolationException e) {
                throw new IllegalStateException(e);
            }
        };
    }
}
