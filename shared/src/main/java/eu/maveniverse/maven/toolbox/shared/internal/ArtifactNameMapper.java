/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.Collection;
import org.eclipse.aether.artifact.Artifact;

/**
 * Mapper that maps artifact onto a string (usually file system friendly).
 */
public interface ArtifactNameMapper {
    String map(Artifact artifact);

    static ArtifactNameMapper compose(ArtifactNameMapper... mappers) {
        return compose(Arrays.asList(mappers));
    }

    static ArtifactNameMapper compose(Collection<ArtifactNameMapper> mappers) {
        return new ArtifactNameMapper() {
            @Override
            public String map(Artifact artifact) {
                String result = "";
                for (ArtifactNameMapper mapper : mappers) {
                    result += mapper.map(artifact);
                }
                return result;
            }
        };
    }

    static ArtifactNameMapper prefix(String prefix) {
        requireNonNull(prefix, "prefix");
        if (prefix.trim().isEmpty()) {
            throw new IllegalArgumentException("invalid prefix");
        }
        return artifact -> prefix;
    }

    static ArtifactNameMapper GACVE() {
        return artifact -> {
            String result = artifact.getGroupId() + ".";
            result += artifact.getArtifactId();
            if (!artifact.getClassifier().isEmpty()) {
                result += "-" + artifact.getClassifier();
            }
            result += "-" + artifact.getVersion();
            result += "." + artifact.getExtension();
            return result;
        };
    }

    static ArtifactNameMapper GACbVE() {
        return artifact -> {
            String result = artifact.getGroupId() + ".";
            result += artifact.getArtifactId();
            if (!artifact.getClassifier().isEmpty()) {
                result += "-" + artifact.getClassifier();
            }
            result += "-" + artifact.getBaseVersion();
            result += "." + artifact.getExtension();
            return result;
        };
    }

    static ArtifactNameMapper GACE() {
        return artifact -> {
            String result = artifact.getGroupId() + ".";
            result += artifact.getArtifactId();
            if (!artifact.getClassifier().isEmpty()) {
                result += "-" + artifact.getClassifier();
            }
            result += "." + artifact.getExtension();
            return result;
        };
    }

    static ArtifactNameMapper GAVE() {
        return artifact -> {
            String result = artifact.getGroupId() + ".";
            result += artifact.getArtifactId();
            result += "-" + artifact.getVersion();
            result += "." + artifact.getExtension();
            return result;
        };
    }

    static ArtifactNameMapper GAbVE() {
        return artifact -> {
            String result = artifact.getGroupId() + ".";
            result += artifact.getArtifactId();
            result += "-" + artifact.getBaseVersion();
            result += "." + artifact.getExtension();
            return result;
        };
    }

    static ArtifactNameMapper GAE() {
        return artifact -> {
            String result = artifact.getGroupId() + ".";
            result += artifact.getArtifactId();
            result += "." + artifact.getExtension();
            return result;
        };
    }

    static ArtifactNameMapper ACVE() {
        return artifact -> {
            String result = artifact.getArtifactId();
            if (!artifact.getClassifier().isEmpty()) {
                result += "-" + artifact.getClassifier();
            }
            result += "-" + artifact.getVersion();
            result += "." + artifact.getExtension();
            return result;
        };
    }

    static ArtifactNameMapper ACbVE() {
        return artifact -> {
            String result = artifact.getArtifactId();
            if (!artifact.getClassifier().isEmpty()) {
                result += "-" + artifact.getClassifier();
            }
            result += "-" + artifact.getBaseVersion();
            result += "." + artifact.getExtension();
            return result;
        };
    }

    static ArtifactNameMapper ACE() {
        return artifact -> {
            String result = artifact.getArtifactId();
            if (!artifact.getClassifier().isEmpty()) {
                result += "-" + artifact.getClassifier();
            }
            result += "." + artifact.getExtension();
            return result;
        };
    }

    static ArtifactNameMapper AVE() {
        return artifact -> {
            String result = artifact.getArtifactId();
            result += "-" + artifact.getVersion();
            result += "." + artifact.getExtension();
            return result;
        };
    }

    static ArtifactNameMapper AbVE() {
        return artifact -> {
            String result = artifact.getArtifactId();
            result += "-" + artifact.getBaseVersion();
            result += "." + artifact.getExtension();
            return result;
        };
    }

    static ArtifactNameMapper AE() {
        return artifact -> {
            String result = artifact.getArtifactId();
            result += "." + artifact.getExtension();
            return result;
        };
    }
}
