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
import eu.maveniverse.maven.toolbox.shared.ReactorLocator;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;

/**
 * Maven project locator.
 */
public class MavenReactorLocator implements ReactorLocator {
    private final ReactorProject topLevel;
    private final ReactorProject current;
    private final List<ReactorProject> allProjects;

    public MavenReactorLocator(MavenSession session, String selector) {
        requireNonNull(session, "session");
        this.allProjects = session.getAllProjects().stream()
                .map(p -> convert(session.getRepositorySession(), p))
                .collect(Collectors.toList());
        this.topLevel = locateProject(
                        RepositoryUtils.toArtifact(session.getTopLevelProject().getArtifact()))
                .orElseThrow();
        if (selector != null) {
            List<MavenProject> candidates = session.getAllProjects().stream()
                    .filter(createSelector(selector))
                    .collect(Collectors.toList());
            if (candidates.size() != 1) {
                List<String> matches = candidates.stream()
                        .map(p -> ArtifactIdUtils.toId(RepositoryUtils.toArtifact(p.getArtifact())))
                        .collect(Collectors.toList());
                throw new IllegalArgumentException("Could not find 1 matching project: " + matches);
            }
            this.current = locateProject(
                            RepositoryUtils.toArtifact(candidates.get(0).getArtifact()))
                    .orElseThrow();
        } else {
            this.current = locateProject(RepositoryUtils.toArtifact(
                            session.getCurrentProject().getArtifact()))
                    .orElseThrow();
        }
    }

    private Predicate<MavenProject> createSelector(String selector) {
        String[] elems =
                Arrays.stream(selector.split(":", -1)).filter(s -> !s.isEmpty()).toArray(String[]::new);
        if (selector.startsWith(":")) {
            // :A or :A:V
            if (elems.length == 1) {
                return p -> p.getArtifactId().equals(elems[0]);
            } else if (elems.length == 2) {
                return p -> p.getArtifactId().equals(elems[0]) && p.getVersion().equals(elems[1]);
            }
        } else {
            // G:A or G:A:V
            if (elems.length == 2) {
                return p -> p.getGroupId().equals(elems[0]) && p.getArtifactId().equals(elems[1]);
            } else if (elems.length == 3) {
                return p -> p.getGroupId().equals(elems[0])
                        && p.getArtifactId().equals(elems[1])
                        && p.getVersion().equals(elems[2]);
            }
        }
        throw new IllegalArgumentException("Unsupported selector expression: '" + selector + "'");
    }

    private ReactorProject convert(RepositorySystemSession session, MavenProject project) {
        requireNonNull(project, "project");
        Artifact pa = RepositoryUtils.toArtifact(project.getArtifact());
        Artifact pp = null;
        if (project.getModel().getParent() != null) {
            Parent parent = project.getModel().getParent();
            pp = new DefaultArtifact(parent.getGroupId(), parent.getArtifactId(), "pom", parent.getVersion());
        }
        List<Dependency> pd = project.getModel().getDependencies().stream()
                .map(d -> RepositoryUtils.toDependency(d, session.getArtifactTypeRegistry()))
                .collect(Collectors.toList());
        List<Artifact> collected = project.getCollectedProjects().stream()
                .map(p -> RepositoryUtils.toArtifact(p.getArtifact()))
                .collect(Collectors.toList());
        return new MProject(pa, pp, pd, collected, this, project.getModel());
    }

    @Override
    public ReactorProject getTopLevelProject() {
        return topLevel;
    }

    @Override
    public ReactorProject getCurrentProject() {
        return current;
    }

    @Override
    public List<ReactorProject> getAllProjects() {
        return allProjects;
    }

    @Override
    public Optional<ReactorProject> locateProject(Artifact artifact) {
        return allProjects.stream()
                .filter(p -> ArtifactIdUtils.equalsId(p.artifact(), artifact))
                .findFirst();
    }

    @Override
    public List<ReactorProject> locateChildren(Project project) {
        return allProjects.stream()
                .filter(p -> {
                    Optional<Artifact> parentArtifact = p.getParent();
                    return parentArtifact.isPresent()
                            && ArtifactIdUtils.equalsId(parentArtifact.orElseThrow(), project.artifact());
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<ReactorProject> locateCollected(Project project) {
        if (project instanceof MProject) {
            MProject mProject = (MProject) project;
            List<Project> allOfGrandchildren = mProject.collected().stream()
                    .map(this::locateProject)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .filter(p -> MProject.class.isAssignableFrom(p.getClass()))
                    .map(p -> ((MProject) p).collected())
                    .flatMap(List::stream)
                    .map(this::locateProject)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());
            return mProject.collected().stream()
                    .map(this::locateProject)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList())
                    .stream()
                    .filter(p -> !allOfGrandchildren.contains(p))
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    @Override
    public Stream<Artifact> get() {
        return getAllProjects().stream().map(Project::artifact);
    }

    private static final class MProject implements ReactorProject {
        private final Artifact artifact;
        private final Artifact parent;
        private final List<Dependency> dependencies;
        private final List<Artifact> collected;
        private final ProjectLocator origin;
        private final Model effectiveModel;

        private MProject(
                Artifact artifact,
                Artifact parent,
                List<Dependency> dependencies,
                List<Artifact> collected,
                ProjectLocator origin,
                Model effectiveModel) {
            this.artifact = requireNonNull(artifact, "artifact");
            this.parent = parent; // nullable
            this.dependencies = requireNonNull(dependencies, "dependencies");
            this.collected = requireNonNull(collected, "collected");
            this.origin = origin;
            this.effectiveModel = requireNonNull(effectiveModel, "effectiveModel");
        }

        @Override
        public Optional<Artifact> getParent() {
            return Optional.ofNullable(parent);
        }

        @Override
        public Artifact artifact() {
            return artifact;
        }

        public Artifact parent() {
            return parent;
        }

        @Override
        public List<Dependency> dependencies() {
            return dependencies;
        }

        public List<Artifact> collected() {
            return collected;
        }

        @Override
        public ProjectLocator origin() {
            return origin;
        }

        @Override
        public Model effectiveModel() {
            return effectiveModel;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            MProject mProject = (MProject) o;
            return Objects.equals(artifact, mProject.artifact)
                    && Objects.equals(parent, mProject.parent)
                    && Objects.equals(dependencies, mProject.dependencies)
                    && Objects.equals(collected, mProject.collected)
                    && Objects.equals(origin, mProject.origin);
        }

        @Override
        public int hashCode() {
            return Objects.hash(artifact, parent, dependencies, collected, origin);
        }
    }
}
