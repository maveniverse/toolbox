/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.toolbox.shared.internal.ToolboxCommandoImpl;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.slf4j.Logger;

/**
 * The Toolbox Commando, that implements all the commands that are exposed via Mojos or CLI.
 * <p>
 * This instance manages {@link Context}, corresponding resolver and search API
 * and maps one-to-one onto commands. Can be considered something like "high level" API of Toolbox.
 * <p>
 * Note on error handling: each "commando" method is marked to throw and return a {@code boolean}.
 * If method cleanly returns, the result shows the "logical success" of the command (think about it {@code false} means
 * "this execution was no-op"). If method throws, {@link RuntimeException} instances (for example NPE, IAEx, ISEx)
 * mark "bad input", or configuration related errors. The checked exception instances on the other hand come from
 * corresponding subsystem like resolver is. Finally, {@link IOException} is thrown on fatal IO problems.
 */
public interface ToolboxCommando {
    /**
     * Gets or creates context. This method should be used to get instance that may be shared
     * across context (session).
     */
    static ToolboxCommando create(Context context) {
        requireNonNull(context, "context");
        return new ToolboxCommandoImpl(context);
    }

    default String getVersion() {
        return ToolboxCommandoVersion.getVersion();
    }

    boolean dump(Logger output);

    // Parsers

    /**
     * Parses artifact mapper string into {@link ArtifactMapper}.
     */
    ArtifactMapper parseArtifactMapperSpec(String spec);

    /**
     * Parses artifact matcher string into {@link ArtifactMatcher}.
     */
    ArtifactMatcher parseArtifactMatcherSpec(String spec);

    /**
     * Parses artifact name mapper string into {@link ArtifactNameMapper}.
     */
    ArtifactNameMapper parseArtifactNameMapperSpec(String spec);

    /**
     * Parses dependency matcher string into {@link DependencyMatcher}.
     */
    DependencyMatcher parseDependencyMatcherSpec(String spec);

    /**
     * Parses artifact version matcher string into {@link ArtifactVersionMatcher}.
     */
    ArtifactVersionMatcher parseArtifactVersionMatcherSpec(String spec);

    /**
     * Parses artifact version selector string into {@link ArtifactVersionSelector}.
     */
    ArtifactVersionSelector parseArtifactVersionSelectorSpec(String spec);

    /**
     * Parses remote repository string into {@link RemoteRepository}. It may be {@code url} only, {@code id::url} or
     * {@code id::type::url} form. In first case, repository ID will be {@code "mima"}.
     */
    RemoteRepository parseRemoteRepository(String spec);

    // Resolver related commands: they target current context contained RemoteRepository

    /**
     * Provides {@link Sink<Artifact>} according to spec.
     */
    Sink<Artifact> artifactSink(String spec);

    /**
     * Provides {@link Sink<Dependency>} according to spec.
     */
    Sink<Dependency> dependencySink(String spec);

    /**
     * Shorthand method, creates {@link ResolutionRoot} out of passed in artifact.
     */
    default ResolutionRoot loadGav(String gav)
            throws InvalidVersionSpecificationException, VersionRangeResolutionException, ArtifactDescriptorException {
        return loadGav(gav, Collections.emptyList());
    }

    /**
     * Shorthand method, creates {@link ResolutionRoot} out of passed in artifact and BOMs.
     */
    ResolutionRoot loadGav(String gav, Collection<String> boms)
            throws InvalidVersionSpecificationException, VersionRangeResolutionException, ArtifactDescriptorException;

    /**
     * Shorthand method, creates collection {@link ResolutionRoot}s out of passed in artifacts and BOMs.
     */
    default Collection<ResolutionRoot> loadGavs(Collection<String> gav, Collection<String> boms)
            throws InvalidVersionSpecificationException, VersionRangeResolutionException, ArtifactDescriptorException {
        List<ResolutionRoot> result = new ArrayList<>(gav.size());
        for (String gavEntry : gav) {
            result.add(loadGav(gavEntry, boms));
        }
        return result;
    }

    /**
     * Converts a dependency into artifact. This may be trivial, but may involve resolving of version range, if
     * dependency uses them.
     */
    Artifact toArtifact(Dependency dependency);

    // Commands

    boolean classpath(ResolutionScope resolutionScope, ResolutionRoot resolutionRoot, Logger output) throws Exception;

    boolean copy(Collection<Artifact> artifacts, Sink<Artifact> sink, Logger output) throws Exception;

    boolean copyTransitive(
            ResolutionScope resolutionScope,
            Collection<ResolutionRoot> resolutionRoots,
            Sink<Artifact> sink,
            Logger output)
            throws Exception;

    boolean copyAllRecorded(Sink<Artifact> sink, boolean stopRecording, Logger output) throws Exception;

    boolean deploy(RemoteRepository remoteRepository, Source<Artifact> artifactSource, Logger output) throws Exception;

    boolean deployAllRecorded(RemoteRepository remoteRepository, boolean stopRecording, Logger output) throws Exception;

    boolean install(Source<Artifact> artifactSource, Logger output) throws Exception;

    default boolean listRepositories(
            ResolutionScope resolutionScope, String context, ResolutionRoot resolutionRoot, Logger output)
            throws Exception {
        HashMap<String, ResolutionRoot> resolutionRoots = new HashMap<>();
        resolutionRoots.put(context, resolutionRoot);
        return listRepositories(resolutionScope, resolutionRoots, output);
    }

    boolean listRepositories(
            ResolutionScope resolutionScope, Map<String, ResolutionRoot> resolutionRoots, Logger output)
            throws Exception;

    boolean listAvailablePlugins(Collection<String> groupIds, Logger output) throws Exception;

    boolean recordStart(Logger output);

    boolean recordStats(Logger output);

    boolean recordStop(Logger output);

    boolean resolve(
            Collection<Artifact> artifacts,
            boolean sources,
            boolean javadoc,
            boolean signature,
            Sink<Artifact> sink,
            Logger output)
            throws Exception;

    boolean resolveTransitive(
            ResolutionScope resolutionScope,
            Collection<ResolutionRoot> resolutionRoots,
            boolean sources,
            boolean javadoc,
            boolean signature,
            Sink<Artifact> sink,
            Logger output)
            throws Exception;

    boolean tree(ResolutionScope resolutionScope, ResolutionRoot resolutionRoot, boolean verboseTree, Logger output)
            throws Exception;

    boolean treeFind(
            ResolutionScope resolutionScope,
            ResolutionRoot resolutionRoot,
            boolean verboseTree,
            ArtifactMatcher artifactMatcher,
            Logger output)
            throws Exception;

    boolean dmList(ResolutionRoot resolutionRoot, boolean verboseList, Logger output) throws Exception;

    boolean dmTree(ResolutionRoot resolutionRoot, boolean verboseTree, Logger output) throws Exception;

    // Search API related commands: they target one single RemoteRepository

    /**
     * Returns a list of known <em>search remote repositories</em>. These {@link RemoteRepository} are NOT usable with
     * Resolver (i.e. to deploy to them), only with Search API!
     */
    Map<String, RemoteRepository> getKnownSearchRemoteRepositories();

    boolean exists(
            RemoteRepository remoteRepository,
            String gav,
            boolean pom,
            boolean sources,
            boolean javadoc,
            boolean signature,
            boolean allRequired,
            String repositoryVendor,
            Logger output)
            throws IOException;

    boolean identify(RemoteRepository remoteRepository, Collection<String> targets, boolean decorated, Logger output)
            throws IOException;

    boolean list(RemoteRepository remoteRepository, String gavoid, String repositoryVendor, Logger output)
            throws IOException;

    boolean search(RemoteRepository remoteRepository, String expression, Logger output) throws IOException;

    boolean verify(RemoteRepository remoteRepository, String gav, String sha1, String repositoryVendor, Logger output)
            throws IOException;

    // Various

    boolean libYear(
            String subject,
            ResolutionScope resolutionScope,
            ResolutionRoot resolutionRoot,
            boolean transitive,
            boolean quiet,
            boolean upToDate,
            Predicate<Version> versionPredicate,
            BiFunction<Artifact, List<Version>, String> artifactVersionSelector,
            String repositoryVendor,
            Logger output)
            throws Exception;

    boolean versions(String context, Collection<Artifact> artifacts, Predicate<Version> versionPredicate, Logger output)
            throws Exception;
}
