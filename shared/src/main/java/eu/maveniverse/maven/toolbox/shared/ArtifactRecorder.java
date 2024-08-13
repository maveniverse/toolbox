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
import java.util.stream.Stream;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * Artifact recorder.
 */
public interface ArtifactRecorder extends ArtifactSource {
    /**
     * The "sentinel" remote repository that recorder assigns when there is no remote repository available. This
     * means that artifact was either installed locally. To check for "sentinel" remote repository use instance
     * equality.
     */
    RemoteRepository SENTINEL = new RemoteRepository.Builder("sentinel", "default", "none").build();

    /**
     * Tells is recorder active or not.
     */
    boolean isActive();

    /**
     * Activate/deactivate recorder.
     */
    boolean setActive(boolean active);

    /**
     * Returns the count of recorded artifacts.
     */
    int recordedCount();

    /**
     * Clears the recorder buffer.
     */
    void clear();

    /**
     * Returns the map of recorded artifacts. Map key may be {@link #SENTINEL}, and it is left at caller discretion
     * how to handle those.
     */
    Map<RemoteRepository, List<Artifact>> getRecordedArtifacts();

    @Override
    default Stream<Artifact> get() {
        return getRecordedArtifacts().values().stream().flatMap(Collection::stream);
    }

    @Override
    default List<Artifact> getAllArtifacts() {
        try (Stream<Artifact> stream = get()) {
            return stream.collect(Collectors.toList());
        }
    }
}
