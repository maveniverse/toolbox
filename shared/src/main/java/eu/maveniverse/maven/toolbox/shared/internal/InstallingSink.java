/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.toolbox.shared.ArtifactSink;
import java.util.Collection;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.installation.InstallRequest;
import org.eclipse.aether.installation.InstallationException;
import org.eclipse.aether.repository.LocalRepository;

/**
 * Construction to accept collection of artifacts and install them into local repository.
 * <p>
 * Note about this sink: while it does use the Resolver API to install artifacts, this sink should never be directed
 * onto your "real" local repository, as it can lead to unexpected results. Installed artifacts with this sink will
 * appear as "locally built" ones, as "install" operation means exactly that: install built artifacts.
 */
public final class InstallingSink implements ArtifactSink {
    /**
     * Creates installing sink that installs into passed in session local repository.
     */
    public static InstallingSink installing(RepositorySystem system, RepositorySystemSession session) {
        return new InstallingSink(system, session);
    }

    private final RepositorySystem system;
    private final RepositorySystemSession session;
    private final InstallRequest installRequest;

    private InstallingSink(RepositorySystem system, RepositorySystemSession session) {
        this.system = requireNonNull(system, "system");
        this.session = requireNonNull(session, "session");
        this.installRequest = new InstallRequest();
        this.installRequest.setTrace(RequestTrace.newChild(null, this));
    }

    public LocalRepository getLocalRepository() {
        return session.getLocalRepository();
    }

    @Override
    public void accept(Collection<Artifact> artifacts) {
        requireNonNull(artifacts, "artifacts");
        installRequest.setArtifacts(artifacts);
    }

    @Override
    public void accept(Artifact artifact) {
        requireNonNull(artifact, "artifact");
        installRequest.addArtifact(artifact);
    }

    @Override
    public void close() throws InstallationException {
        system.install(session, installRequest);
    }
}
