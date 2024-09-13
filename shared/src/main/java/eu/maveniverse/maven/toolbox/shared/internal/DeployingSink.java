/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.deployment.DeploymentException;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * Construction to accept collection of artifacts and deploy them into given remote repository.
 */
public final class DeployingSink extends ArtifactSink {
    /**
     * Creates installing sink that installs into passed in session local repository.
     */
    public static DeployingSink deploying(
            RepositorySystem system, RepositorySystemSession session, RemoteRepository repository) {
        return new DeployingSink(system, session, repository);
    }

    private final RepositorySystem system;
    private final RepositorySystemSession session;
    private final DeployRequest deployRequest;

    private DeployingSink(RepositorySystem system, RepositorySystemSession session, RemoteRepository repository) {
        this.system = requireNonNull(system, "system");
        this.session = requireNonNull(session, "session");
        this.deployRequest = new DeployRequest();
        this.deployRequest.setRepository(repository);
        this.deployRequest.setTrace(RequestTrace.newChild(null, this));
    }

    public RemoteRepository getRemoteRepository() {
        return deployRequest.getRepository();
    }

    @Override
    public void accept(Collection<Artifact> artifacts) {
        requireNonNull(artifacts, "artifacts");
        deployRequest.setArtifacts(artifacts);
    }

    @Override
    public void accept(Artifact artifact) {
        requireNonNull(artifact, "artifact");
        deployRequest.addArtifact(artifact);
    }

    @Override
    public void close() throws DeploymentException {
        system.deploy(session, deployRequest);
    }
}
