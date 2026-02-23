/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import eu.maveniverse.maven.toolbox.shared.ArtifactRecorder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;

public class ArtifactRecorderImpl extends AbstractRepositoryListener implements Artifacts.Source, ArtifactRecorder {
    private final ConcurrentHashMap<RemoteRepository, List<Artifact>> recordedArtifacts;
    private final AtomicBoolean active;

    public ArtifactRecorderImpl() {
        this.recordedArtifacts = new ConcurrentHashMap<>();
        this.active = new AtomicBoolean(false);
    }

    @Override
    public void artifactResolved(RepositoryEvent event) {
        if (active.get()) {
            RemoteRepository repository = event.getRepository() instanceof RemoteRepository
                    ? (RemoteRepository) event.getRepository()
                    : SENTINEL;
            recordedArtifacts
                    .computeIfAbsent(repository, k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(event.getArtifact());
        }
    }

    @Override
    public boolean isActive() {
        return active.get();
    }

    @Override
    public boolean setActive(boolean val) {
        return active.compareAndSet(!val, val);
    }

    @Override
    public int recordedCount() {
        return recordedArtifacts.values().stream().mapToInt(Collection::size).sum();
    }

    @Override
    public void clear() {
        recordedArtifacts.clear();
    }

    @Override
    public Map<RemoteRepository, List<Artifact>> getRecordedArtifacts() {
        return recordedArtifacts;
    }
}
