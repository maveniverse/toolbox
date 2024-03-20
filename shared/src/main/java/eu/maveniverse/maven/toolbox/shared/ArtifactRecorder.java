/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * Artifact recorder.
 */
public interface ArtifactRecorder {
    /**
     * The "sentinel" remote repository that recorder assigns when there is no remote repository available. This
     * means that artifact was either installed locally. To check for "sentinel" remote repository use instance
     * equality.
     */
    RemoteRepository SENTINEL = new RemoteRepository.Builder("sentinel", "default", "none").build();

    /**
     * Activate/deactivate recorder.
     */
    boolean setActive(boolean active);

    /**
     * Clears the recorder buffer.
     */
    void clear();

    /**
     * Returns the map of recorded artifacts. Map key may be {@link #SENTINEL}, and it is left at caller discretion
     * how to handle those.
     */
    Map<RemoteRepository, List<Artifact>> getRecordedArtifacts();

    default List<Artifact> getAllArtifacts() {
        return getRecordedArtifacts().values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }
}
