/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

import static java.util.Objects.requireNonNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

/**
 * Construction to group artifacts, make them "like a project is".
 * <p>
 * Warning: this abstraction uses extension and not packaging, as this one does not create POM, it is caller obligation.
 */
public final class ProjectArtifacts implements Supplier<Collection<Artifact>> {
    private final String groupId;
    private final String artifactId;
    private final String version;
    private final String extension;
    private final Map<CE, Path> artifacts;

    public ProjectArtifacts(String gav) {
        DefaultArtifact prototype = new DefaultArtifact(gav);
        this.groupId = prototype.getGroupId();
        this.artifactId = prototype.getArtifactId();
        this.version = prototype.getVersion();
        this.extension = prototype.getExtension();
        this.artifacts = new HashMap<>();
    }

    public void addPom(Path artifact) {
        addArtifact(null, "pom", artifact);
    }

    public void addMain(Path artifact) {
        addArtifact(null, extension, artifact);
    }

    public void addSources(Path artifact) {
        addArtifact("sources", "jar", artifact);
    }

    public void addJavadoc(Path artifact) {
        addArtifact("javadoc", "jar", artifact);
    }

    public void addArtifact(String classifier, String extension, Path artifact) {
        requireNonNull(extension, "extension");
        requireNonNull(artifact, "artifact");
        if (!Files.exists(artifact) || Files.isDirectory(artifact)) {
            throw new IllegalArgumentException("artifact backing file must exist and cannot be a directory");
        }
        CE ce = new CE(classifier, extension);
        if (artifacts.containsKey(ce)) {
            throw new IllegalArgumentException("artifact already present");
        }
        artifacts.put(ce, artifact);
    }

    @Override
    public List<Artifact> get() {
        ArrayList<Artifact> result = new ArrayList<>(artifacts.size());
        artifacts.forEach((ce, path) -> result.add(
                new DefaultArtifact(groupId, artifactId, ce.classifier, ce.extension, version).setFile(path.toFile())));
        return result;
    }

    private static final class CE {
        private final String classifier;
        private final String extension;

        private CE(String classifier, String extension) {
            this.classifier = classifier;
            this.extension = requireNonNull(extension, "extension");
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            CE ce = (CE) o;
            return Objects.equals(classifier, ce.classifier) && Objects.equals(extension, ce.extension);
        }

        @Override
        public int hashCode() {
            return Objects.hash(classifier, extension);
        }
    }
}
