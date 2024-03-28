/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * Construction to accept collection of artifacts and purge them from local repository.
 */
public final class PurgingSink implements ArtifactSink {
    /**
     * Creates purging sink treats artifacts as "whole", purges whole GAVs from passed in session local repository.
     * Artifacts this sink accepts MUST NOT BE resolved from the same local repository this purging sink is about to
     * purge.
     */
    public static PurgingSink purging(
            Output output,
            RepositorySystem system,
            RepositorySystemSession session,
            List<RemoteRepository> remoteRepositories) {
        return purging(output, Mode.WHOLE, false, system, session, remoteRepositories);
    }

    /**
     * Creates purging sink that purges from passed in session local repository.
     */
    public static PurgingSink purging(
            Output output,
            Mode mode,
            boolean enforceOrigin,
            RepositorySystem system,
            RepositorySystemSession session,
            List<RemoteRepository> remoteRepositories) {
        return new PurgingSink(output, mode, enforceOrigin, system, session, remoteRepositories);
    }

    /**
     * Purging mode.
     */
    public enum Mode {
        /**
         * Purge exact artifacts.
         * <p>
         * This mode purges <em>exactly</em> the selected artifacts (i.e. "artifact-1.0-sources.jar") with all its
         * sub-related files (hashes, signatures, cached not-found records), local repository registration, and finally
         * removes all related remote and local metadata caches forcing Maven to re-fetch them.
         */
        EXACT,

        /**
         * Purge GAV, treat artifacts as "whole". Important: for snapshots cached from remote this has important
         * implications! It will delete ALL cached timestamped snapshots!
         * <p>
         * This mode purges <am>whole GAV</am>, treats artifacts as "one": removes complete GAV, so all POM, main and
         * classified artifacts along with all metadata. This mode is the preferred way of purging and is the most
         * "future-proof" mode.
         */
        WHOLE
    }

    private final Output output;
    private final Mode mode;
    private final boolean enforceOrigin;
    private final AtomicBoolean perform;
    private final RepositorySystem system;
    private final RepositorySystemSession session;
    private final List<RemoteRepository> remoteRepositories;
    private final ArrayList<Artifact> artifacts;
    private final Predicate<Artifact> artifactMatcher;

    private PurgingSink(
            Output output,
            Mode mode,
            boolean enforceOrigin,
            RepositorySystem system,
            RepositorySystemSession session,
            List<RemoteRepository> remoteRepositories) {
        this.output = requireNonNull(output, "output");
        this.mode = requireNonNull(mode, "mode");
        this.enforceOrigin = enforceOrigin;
        this.perform = new AtomicBoolean(true);
        this.system = requireNonNull(system, "system");
        this.session = requireNonNull(session, "session");
        this.remoteRepositories = requireNonNull(remoteRepositories, "remoteRepositories");
        this.artifacts = new ArrayList<>();

        // Note: delimiters, while MAY look superfluous, are actually required differentiate a.b:c.d and a.b.c:d
        switch (mode) {
            case EXACT:
                this.artifactMatcher = ArtifactMatcher.unique();
                break;
            case WHOLE:
                this.artifactMatcher = ArtifactMatcher.uniqueBy(ArtifactNameMapper.GAVKey());
                break;
            default:
                throw new IllegalArgumentException("unknown mode");
        }
    }

    public LocalRepository getLocalRepository() {
        return session.getLocalRepository();
    }

    @Override
    public void accept(Artifact artifact) {
        requireNonNull(artifact, "artifact");
        if (artifactMatcher.test(artifact)) {
            requireNonNull(artifact.getFile(), "unresolved artifact");
            if (enforceOrigin
                    && !artifact.getFile()
                            .toPath()
                            .startsWith(
                                    session.getLocalRepository().getBasedir().toPath())) {
                throw new IllegalArgumentException("artifact does not originate from local repository to be purged");
            }
            artifacts.add(artifact);
        }
    }

    @Override
    public void cleanup(Exception e) {
        perform.set(false);
    }

    @Override
    public void close() throws IOException {
        if (perform.get()) {
            int artifactCount = 0;
            output.verbose(
                    "Purging {} artifacts from local repository {}...", artifacts.size(), session.getLocalRepository());
            for (Artifact artifact : artifacts) {
                artifactCount += purgeArtifact(artifact) ? 1 : 0;
            }
            output.normal("Purged {} artifacts from local repository...", artifactCount);
        }
    }

    private boolean purgeArtifact(Artifact artifact) throws IOException {
        switch (mode) {
            case EXACT:
                return purgeExact(artifact);
            case WHOLE:
                return purgeGAV(artifact);
            default:
                throw new IllegalStateException("unknown mode");
        }
    }

    private boolean purgeExact(Artifact artifact) throws IOException {
        // maintain repository state
        // purge artifact file and additional sub-files (hashes, signatures, lastUpdated...)
        int deleted = 0;
        Path path = session.getLocalRepository()
                .getBasedir()
                .toPath()
                .resolve(session.getLocalRepositoryManager().getPathForLocalArtifact(artifact));
        if (Files.isDirectory(path.getParent())) {
            unregisterArtifact(path);
            resetMetadata(
                    path.getParent(),
                    artifact.isSnapshot() && Objects.equals(artifact.getVersion(), artifact.getBaseVersion()));
            deleted = deleteFileAndSubs(session.getLocalRepository()
                    .getBasedir()
                    .toPath()
                    .resolve(session.getLocalRepositoryManager().getPathForLocalArtifact(artifact)));
        }
        for (RemoteRepository repository : remoteRepositories) {
            path = session.getLocalRepository()
                    .getBasedir()
                    .toPath()
                    .resolve(session.getLocalRepositoryManager().getPathForRemoteArtifact(artifact, repository, null));
            if (Files.isDirectory(path.getParent())) {
                unregisterArtifact(path);
                resetMetadata(
                        path.getParent(),
                        artifact.isSnapshot() && Objects.equals(artifact.getVersion(), artifact.getBaseVersion()));
                deleted += deleteFileAndSubs(session.getLocalRepository()
                        .getBasedir()
                        .toPath()
                        .resolve(session.getLocalRepositoryManager().getPathForLocalArtifact(artifact)));
            }
        }
        return deleted != 0;
    }

    private boolean purgeGAV(Artifact artifact) throws IOException {
        // purge artifact GAV directory (but watch out for subdirectories)
        // no need to maintain anything as whole directory is gone
        return deleteDirectory(session.getLocalRepository()
                        .getBasedir()
                        .toPath()
                        .resolve(session.getLocalRepositoryManager().getPathForLocalArtifact(artifact))
                        .getParent())
                != 0;
    }

    private void unregisterArtifact(Path artifactPath) throws IOException {
        // unregister
        Path registrarPath = artifactPath.getParent().resolve("_remote.repositories");
        if (Files.isRegularFile(registrarPath)) {
            String keyPrefix = artifactPath.getFileName().toString() + ">";
            Properties registrar = new Properties();
            try (InputStream input = Files.newInputStream(registrarPath)) {
                registrar.load(input);
            }
            for (String key : new HashSet<>(registrar.stringPropertyNames())) {
                if (key.startsWith(keyPrefix)) {
                    registrar.remove(key);
                }
            }
            try (OutputStream output = Files.newOutputStream(registrarPath)) {
                registrar.store(
                        output,
                        "#NOTE: This is a Maven Resolver internal implementation file, its format can be changed without prior notice.");
            }
        }
    }

    private void resetMetadata(Path directory, boolean local) throws IOException {
        // delete all "maven-metadata-*.xml" except "maven-metadata-local.xml" if local == false
        try (DirectoryStream<Path> toBeDeleted = Files.newDirectoryStream(
                directory,
                p -> Files.exists(p)
                        && !Files.isDirectory(p)
                        && p.getFileName().startsWith("maven-metadata-")
                        && p.getFileName().endsWith(".xml"))) {
            for (Path p : toBeDeleted) {
                if (!local && "maven-metadata-local.xml".equals(p.getFileName().toString())) {
                    continue;
                }
                Files.delete(p);
            }
        }

        // delete "resolver-status.properties"
        Files.deleteIfExists(directory.resolve("resolver-status.properties"));
    }

    private int deleteFileAndSubs(Path path) throws IOException {
        int deleted = 0;
        if (Files.isDirectory(path.getParent())) {
            try (DirectoryStream<Path> toBeDeleted = Files.newDirectoryStream(
                    path.getParent(), p -> Files.exists(p) && !Files.isDirectory(p) && p.startsWith(path))) {
                for (Path p : toBeDeleted) {
                    Files.delete(p);
                    deleted++;
                }
            }
        }
        return deleted;
    }

    private int deleteDirectory(Path directory) throws IOException {
        int found = 0;
        int deleted = 0;
        if (Files.isDirectory(directory)) {
            try (DirectoryStream<Path> toBeDeleted = Files.newDirectoryStream(directory)) {
                for (Path p : toBeDeleted) {
                    found++;
                    if (!Files.isDirectory(p)) {
                        Files.delete(p);
                        deleted++;
                    }
                }
            }
            if (found == deleted) {
                Files.delete(directory);
            }
        }
        return deleted;
    }
}
