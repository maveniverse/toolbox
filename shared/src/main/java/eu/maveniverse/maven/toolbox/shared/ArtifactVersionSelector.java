/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

import java.util.List;
import java.util.function.BiFunction;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.version.Version;

/**
 * Selector that selects artifact version.
 */
public interface ArtifactVersionSelector extends BiFunction<Artifact, List<Version>, String> {
    /**
     * Selector that returns artifact version.
     */
    static ArtifactVersionSelector identity() {
        return new ArtifactVersionSelector() {
            @Override
            public String apply(Artifact artifact, List<Version> versions) {
                return artifact.getVersion();
            }
        };
    }

    /**
     * Selector that return plan last version.
     */
    static ArtifactVersionSelector last() {
        return new ArtifactVersionSelector() {
            @Override
            public String apply(Artifact artifact, List<Version> versions) {
                return versions.isEmpty()
                        ? identity().apply(artifact, versions)
                        : versions.get(versions.size() - 1).toString();
            }
        };
    }
}
