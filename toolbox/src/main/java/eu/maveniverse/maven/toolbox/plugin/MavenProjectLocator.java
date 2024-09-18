/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.toolbox.shared.ProjectLocator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Parent;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;

/**
 * Maven project locator.
 */
public class MavenProjectLocator implements ProjectLocator {
    private final MavenSession session;
    private final Project root;
    private final Project current;
    private final List<Project> allProjects;

    public MavenProjectLocator(MavenSession session) {
        this.session = requireNonNull(session, "session");
        this.allProjects = session.getAllProjects().stream()
                .map(p -> (Project) new MProject(session, p))
                .toList();
        this.root = locateProject(
                        RepositoryUtils.toArtifact(session.getTopLevelProject().getArtifact()))
                .orElseThrow();
        this.current = locateProject(
                        RepositoryUtils.toArtifact(session.getCurrentProject().getArtifact()))
                .orElseThrow();
    }

    @Override
    public Project getRootProject() {
        return root;
    }

    @Override
    public Project getCurrentProject() {
        return current;
    }

    @Override
    public List<Project> getAllProjects() {
        return allProjects;
    }

    @Override
    public Optional<Project> locateProject(Artifact artifact) {
        return allProjects.stream()
                .filter(p -> ArtifactIdUtils.equalsId(p.getArtifact(), artifact))
                .findFirst();
    }

    @Override
    public List<Project> locateChildren(Project project) {
        return allProjects.stream()
                .filter(p -> {
                    Optional<Artifact> parentArtifact = p.getParent();
                    return parentArtifact.isPresent()
                            && ArtifactIdUtils.equalsId(parentArtifact.orElseThrow(), project.getArtifact());
                })
                .toList();
    }

    @Override
    public Stream<Artifact> get() {
        return session.getAllProjects().stream().map(p -> RepositoryUtils.toArtifact(p.getArtifact()));
    }

    public static class MProject implements Project {
        private final Artifact artifact;
        private final Artifact parent;
        private final List<Dependency> dependencies;

        private MProject(MavenSession session, MavenProject project) {
            requireNonNull(project, "project");
            this.artifact = RepositoryUtils.toArtifact(project.getArtifact());
            if (project.getModel().getParent() != null) {
                Parent parent = project.getModel().getParent();
                this.parent =
                        new DefaultArtifact(parent.getGroupId(), parent.getArtifactId(), "pom", parent.getVersion());
            } else {
                this.parent = null;
            }
            this.dependencies = project.getDependencies().stream()
                    .map(d -> RepositoryUtils.toDependency(
                            d, session.getRepositorySession().getArtifactTypeRegistry()))
                    .toList();
        }

        @Override
        public Artifact getArtifact() {
            return artifact;
        }

        @Override
        public Optional<Artifact> getParent() {
            return Optional.ofNullable(parent);
        }

        @Override
        public List<Dependency> getDependencies() {
            return dependencies;
        }
    }
}
