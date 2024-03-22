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
import java.util.function.Function;
import org.eclipse.aether.artifact.Artifact;

/**
 * Mapper that maps artifact onto a string (usually file system friendly).
 */
public interface ArtifactNameMapper extends Function<Artifact, String> {
    @Override
    String apply(Artifact artifact);

    static ArtifactNameMapper compose(ArtifactNameMapper... mappers) {
        return compose(Arrays.asList(mappers));
    }

    static ArtifactNameMapper compose(Collection<ArtifactNameMapper> mappers) {
        return new ArtifactNameMapper() {
            @Override
            public String apply(Artifact artifact) {
                String result = "";
                for (ArtifactNameMapper mapper : mappers) {
                    result += mapper.apply(artifact);
                }
                return result;
            }
        };
    }

    static ArtifactNameMapper fixed(String prefix) {
        requireNonNull(prefix, "prefix");
        if (prefix.trim().isEmpty()) {
            throw new IllegalArgumentException("invalid prefix");
        }
        return artifact -> prefix;
    }

    static ArtifactNameMapper optionalPrefix(String prefix, ArtifactNameMapper artifactNameMapper) {
        requireNonNull(prefix, "prefix");
        requireNonNull(artifactNameMapper, "artifactNameMapper");
        if (prefix.trim().isEmpty()) {
            throw new IllegalArgumentException("invalid prefix");
        }
        return artifact -> {
            String val = artifactNameMapper.apply(artifact);
            if (val != null && !val.trim().isEmpty()) {
                return prefix + val;
            }
            return "";
        };
    }

    static ArtifactNameMapper optionalSuffix(String suffix, ArtifactNameMapper artifactNameMapper) {
        requireNonNull(suffix, "suffix");
        requireNonNull(artifactNameMapper, "artifactNameMapper");
        if (suffix.trim().isEmpty()) {
            throw new IllegalArgumentException("invalid suffix");
        }
        return artifact -> {
            String val = artifactNameMapper.apply(artifact);
            if (val != null && !val.trim().isEmpty()) {
                return val + suffix;
            }
            return "";
        };
    }

    static ArtifactNameMapper repositoryDefault(String fs) {
        return artifact -> {
            StringBuilder path = new StringBuilder(128);
            path.append(artifact.getGroupId().replace(".", fs)).append(fs);
            path.append(artifact.getArtifactId()).append(fs);
            path.append(artifact.getBaseVersion()).append(fs);
            path.append(artifact.getArtifactId()).append("-").append(artifact.getVersion());
            if (!artifact.getClassifier().isEmpty()) {
                path.append("-").append(artifact.getClassifier());
            }
            path.append(".").append(artifact.getExtension());
            return path.toString();
        };
    }

    static ArtifactNameMapper G() {
        return Artifact::getGroupId;
    }

    static ArtifactNameMapper A() {
        return Artifact::getArtifactId;
    }

    static ArtifactNameMapper V() {
        return Artifact::getVersion;
    }

    static ArtifactNameMapper bV() {
        return Artifact::getBaseVersion;
    }

    static ArtifactNameMapper C() {
        return Artifact::getClassifier;
    }

    static ArtifactNameMapper E() {
        return Artifact::getExtension;
    }

    static ArtifactNameMapper P(String key, String def) {
        return a -> a.getProperty(key, def);
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
