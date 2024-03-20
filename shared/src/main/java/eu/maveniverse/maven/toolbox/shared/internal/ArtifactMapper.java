/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import java.util.Arrays;
import java.util.Collection;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

public interface ArtifactMapper {
    Artifact map(Artifact artifact);

    static ArtifactMapper compose(ArtifactMapper... mappers) {
        return compose(Arrays.asList(mappers));
    }

    static ArtifactMapper compose(Collection<ArtifactMapper> mappers) {
        return new ArtifactMapper() {
            @Override
            public Artifact map(Artifact artifact) {
                for (ArtifactMapper mapper : mappers) {
                    artifact = mapper.map(artifact);
                }
                return artifact;
            }
        };
    }

    static ArtifactMapper bV() {
        return new ArtifactMapper() {
            @Override
            public Artifact map(Artifact artifact) {
                return new DefaultArtifact(
                        artifact.getGroupId(),
                        artifact.getArtifactId(),
                        artifact.getClassifier(),
                        artifact.getExtension(),
                        artifact.getBaseVersion(),
                        artifact.getProperties(),
                        artifact.getFile());
            }
        };
    }

    static ArtifactMapper woC() {
        return new ArtifactMapper() {
            @Override
            public Artifact map(Artifact artifact) {
                return new DefaultArtifact(
                        artifact.getGroupId(),
                        artifact.getArtifactId(),
                        null,
                        artifact.getExtension(),
                        artifact.getVersion(),
                        artifact.getProperties(),
                        artifact.getFile());
            }
        };
    }

    static ArtifactMapper rename(String g, String a, String v) {
        return new ArtifactMapper() {
            @Override
            public Artifact map(Artifact artifact) {
                return new DefaultArtifact(
                        g != null ? g : artifact.getGroupId(),
                        a != null ? a : artifact.getArtifactId(),
                        artifact.getClassifier(),
                        artifact.getExtension(),
                        v != null ? v : artifact.getVersion(),
                        artifact.getProperties(),
                        artifact.getFile());
            }
        };
    }
}
