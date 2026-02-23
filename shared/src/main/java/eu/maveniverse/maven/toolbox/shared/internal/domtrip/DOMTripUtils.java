/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal.domtrip;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

/**
 * Utils.
 */
public final class DOMTripUtils {
    private DOMTripUtils() {}

    public static Artifact toResolver(eu.maveniverse.domtrip.maven.Coordinates artifact) {
        return new DefaultArtifact(
                artifact.groupId(), artifact.artifactId(), artifact.classifier(), artifact.type(), artifact.version());
    }

    public static eu.maveniverse.domtrip.maven.Coordinates toDomTrip(Artifact artifact) {
        return eu.maveniverse.domtrip.maven.Coordinates.of(
                artifact.getGroupId(),
                artifact.getArtifactId(),
                artifact.getVersion(),
                artifact.getClassifier(),
                artifact.getExtension());
    }
}
