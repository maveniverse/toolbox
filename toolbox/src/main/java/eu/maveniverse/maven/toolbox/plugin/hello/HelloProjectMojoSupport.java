/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.hello;

import eu.maveniverse.domtrip.maven.Coordinates;
import eu.maveniverse.maven.toolbox.shared.internal.domtrip.DOMTripUtils;
import java.nio.file.Files;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

/**
 * Support class for "hello" Mojos (with existing project).
 */
public abstract class HelloProjectMojoSupport extends HelloMojoSupport {
    protected Artifact getCurrentProjectArtifact() {
        if (!Files.exists(getRootPom())) {
            throw new IllegalStateException("This directory does not contain pom.xml");
        }
        return DOMTripUtils.toResolver(Coordinates.fromPom(getRootPom()));
    }

    /**
     * Accepts {@code A}, {@code G:A} or {@code G:A:V}. Missing pieces are combined with current project.
     */
    protected Artifact toSubProjectArtifact(String gav) {
        Artifact currentProjectArtifact = getCurrentProjectArtifact();
        Artifact result;
        try {
            result = new DefaultArtifact(gav);
        } catch (IllegalArgumentException ex) {
            try {
                result = new DefaultArtifact(gav + ":" + currentProjectArtifact.getVersion());
            } catch (IllegalArgumentException ex2) {
                try {
                    if (gav.startsWith(".") && gav.contains(":")) {
                        result = new DefaultArtifact(
                                currentProjectArtifact.getGroupId() + gav + ":" + currentProjectArtifact.getVersion());
                    } else {
                        result = new DefaultArtifact(currentProjectArtifact.getGroupId() + ":" + gav + ":"
                                + currentProjectArtifact.getVersion());
                    }
                } catch (IllegalArgumentException ex3) {
                    throw new IllegalArgumentException("Invalid gav: " + gav);
                }
            }
        }
        return result.setFile(getRootPom()
                .getParent()
                .resolve(result.getArtifactId())
                .resolve("pom.xml")
                .toFile());
    }
}
