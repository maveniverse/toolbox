/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.toolbox.shared.internal.maven.ArtifactDescriptorReaderImpl;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.repository.internal.ModelCacheFactory;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.impl.RepositoryEventDispatcher;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToolboxMavenImpl {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ArtifactDescriptorReaderImpl artifactDescriptorReader;
    private final RepositorySystemSession session;

    public ToolboxMavenImpl(
            RepositorySystem repositorySystem,
            RepositorySystemSession session,
            RemoteRepositoryManager remoteRepositoryManager,
            ModelBuilder modelBuilder,
            RepositoryEventDispatcher repositoryEventDispatcher,
            ModelCacheFactory modelCacheFactory) {
        this.artifactDescriptorReader = new ArtifactDescriptorReaderImpl(
                repositorySystem, remoteRepositoryManager, modelBuilder, repositoryEventDispatcher, modelCacheFactory);
        this.session = requireNonNull(session, "session");
    }

    public ArtifactDescriptorResult readEffectiveModel(ArtifactDescriptorRequest request)
            throws ArtifactDescriptorException {
        return artifactDescriptorReader.readEffectiveArtifactDescriptor(session, request);
    }

    public ArtifactDescriptorResult readRawModel(ArtifactDescriptorRequest request) throws ArtifactDescriptorException {
        return artifactDescriptorReader.readRawArtifactDescriptor(session, request);
    }
}
