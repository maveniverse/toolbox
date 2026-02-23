/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mima.extensions.mmr.MavenModelReader;
import eu.maveniverse.maven.mima.extensions.mmr.ModelRequest;
import eu.maveniverse.maven.mima.extensions.mmr.ModelResponse;
import eu.maveniverse.maven.toolbox.shared.ProjectLocator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.ArtifactType;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;

public class ProjectLocatorImpl implements ProjectLocator {
    private final RepositorySystemSession session;
    private final MavenModelReader mavenModelReader;

    public ProjectLocatorImpl(RepositorySystemSession session, MavenModelReader mavenModelReader) {
        this.session = requireNonNull(session, "session");
        this.mavenModelReader = requireNonNull(mavenModelReader, "mavenModelReader");
    }

    @Override
    public Optional<Project> locateProject(Artifact artifact) {
        try {
            ModelResponse modelResponse = mavenModelReader.readModel(ModelRequest.builder()
                    .setArtifact(artifact)
                    .setRequestContext("projectLocator")
                    .setTrace(new RequestTrace(artifact))
                    .build());

            Model em = modelResponse.getEffectiveModel();
            String ext = em.getPackaging();
            ArtifactType type = session.getArtifactTypeRegistry().get(em.getPackaging());
            if (type != null) {
                ext = type.getExtension();
            }
            Artifact pa = new DefaultArtifact(em.getGroupId(), em.getArtifactId(), ext, em.getVersion());
            Artifact pp = null;
            if (em.getParent() != null) {
                Parent ep = em.getParent();
                pp = new DefaultArtifact(ep.getGroupId(), ep.getArtifactId(), "pom", ep.getVersion());
            }
            List<Dependency> pd = em.getDependencies().stream()
                    .map(d -> RepositoryUtils.toDependency(d, session.getArtifactTypeRegistry()))
                    .collect(Collectors.toList());
            return Optional.of(new ModelProject(pa, pp, pd, this));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static final class ModelProject implements Project {
        private final Artifact artifact;
        private final Artifact parent;
        private final List<Dependency> dependencies;
        private final ProjectLocator origin;

        private ModelProject(Artifact artifact, Artifact parent, List<Dependency> dependencies, ProjectLocator origin) {
            this.artifact = requireNonNull(artifact, "artifact");
            this.parent = parent; // nullable
            this.dependencies = requireNonNull(dependencies, "dependencies");
            this.origin = origin;
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

        @Override
        public ProjectLocator origin() {
            return origin;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            ModelProject that = (ModelProject) obj;
            return Objects.equals(this.artifact, that.artifact)
                    && Objects.equals(this.parent, that.parent)
                    && Objects.equals(this.dependencies, that.dependencies)
                    && Objects.equals(this.origin, that.origin);
        }

        @Override
        public int hashCode() {
            return Objects.hash(artifact, parent, dependencies, origin);
        }

        @Override
        public String toString() {
            return "ModelProject[" + "artifact="
                    + artifact + ", " + "parent="
                    + parent + ", " + "dependencies="
                    + dependencies + ", " + "origin="
                    + origin + ']';
        }
    }
}
