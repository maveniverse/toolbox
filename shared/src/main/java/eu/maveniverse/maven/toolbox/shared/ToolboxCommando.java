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
import eu.maveniverse.maven.mima.context.Runtime;
import eu.maveniverse.maven.toolbox.shared.internal.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;

/**
 * The Toolbox Commando, that implements all the commands that are exposed via Mojos or CLI.
 * <p>
 * This instance manages {@link Context}, corresponding {@link ToolboxResolver} and {@link ToolboxSearchApi}
 * and maps one-to-one onto commands. Can be considered something like "high level" API of Toolbox.
 * <p>
 * Note on error handling: each "commando" method is marked to throw and return a {@link boolean}.
 * If method cleanly returns, the result shows the "logical success" of the command (think about it {@code false} means
 * "this execution was no-op"). If method throws, {@link RuntimeException} instances (for example NPE, IAEx, ISEx)
 * mark "bad input", or configuration related errors. The checked exception instances on the other hand come from
 * corresponding subsystem like resolver is. Finally, {@link IOException} is thrown on fatal IO problems.
 */
public interface ToolboxCommando {
    /**
     * Gets or creates context. This method should be used to get {@link ToolboxResolver} instance that may be shared
     * across context (session).
     */
    static ToolboxCommando create(Runtime runtime, Context context) {
        requireNonNull(runtime, "runtime");
        requireNonNull(context, "context");
        return new ToolboxCommandoImpl(runtime, context);
    }

    default String getVersion() {
        return ToolboxCommandoVersion.getVersion();
    }

    boolean dump(boolean verbose, Output output);

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
     * Parses remote repository string into {@link RemoteRepository}. It may be {@code url} only, {@code id::url} or
     * {@code id::type::url} form. In first case, repository ID will be {@code "mima"}.
     */
    RemoteRepository parseRemoteRepository(String spec);

    // Resolver related commands: they target current context contained RemoteRepository

    /**
     * Provides {@link ArtifactSink} according to spec.
     */
    ArtifactSink artifactSink(Output output, String spec) throws IOException;

    /**
     * Shorthand method, creates {@link ResolutionRoot} out of passed in artifact.
     */
    default ResolutionRoot loadGav(String gav) throws ArtifactDescriptorException {
        return loadGav(gav, Collections.emptyList());
    }

    /**
     * Shorthand method, creates {@link ResolutionRoot} out of passed in artifact and BOMs.
     *
     * @see ToolboxResolver#loadGav(String, Collection)
     */
    ResolutionRoot loadGav(String gav, Collection<String> boms) throws ArtifactDescriptorException;

    /**
     * Shorthand method, creates collection {@link ResolutionRoot}s out of passed in artifacts and BOMs.
     */
    default Collection<ResolutionRoot> loadGavs(Collection<String> gav, Collection<String> boms)
            throws ArtifactDescriptorException {
        List<ResolutionRoot> result = new ArrayList<>(gav.size());
        for (String gavEntry : gav) {
            result.add(loadGav(gavEntry, boms));
        }
        return result;
    }

    boolean classpath(ResolutionScope resolutionScope, ResolutionRoot resolutionRoot, Output output) throws Exception;

    boolean copy(Collection<Artifact> artifacts, ArtifactSink sink, Output output) throws Exception;

    boolean copyTransitive(
            ResolutionScope resolutionScope,
            Collection<ResolutionRoot> resolutionRoots,
            ArtifactSink sink,
            Output output)
            throws Exception;

    boolean deploy(String remoteRepositorySpec, Supplier<Collection<Artifact>> artifactSupplier, Output output)
            throws Exception;

    boolean deployAllRecorded(String remoteRepositorySpec, boolean stopRecording, Output output) throws Exception;

    boolean install(Supplier<Collection<Artifact>> artifactSupplier, Output output) throws Exception;

    boolean listRepositories(ResolutionScope resolutionScope, ResolutionRoot resolutionRoot, Output output)
            throws Exception;

    boolean listAvailablePlugins(Collection<String> groupIds, Output output) throws Exception;

    boolean recordStart(Output output);

    boolean recordStats(Output output);

    boolean recordStop(Output output);

    boolean resolve(
            Collection<Artifact> artifacts,
            boolean sources,
            boolean javadoc,
            boolean signature,
            ArtifactSink sink,
            Output output)
            throws Exception;

    boolean resolveTransitive(
            ResolutionScope resolutionScope,
            Collection<ResolutionRoot> resolutionRoots,
            boolean sources,
            boolean javadoc,
            boolean signature,
            ArtifactSink sink,
            Output output)
            throws Exception;

    boolean tree(ResolutionScope resolutionScope, ResolutionRoot resolutionRoot, boolean verbose, Output output)
            throws Exception;

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
            Output output)
            throws IOException;

    boolean identify(RemoteRepository remoteRepository, String target, Output output) throws IOException;

    boolean list(RemoteRepository remoteRepository, String gavoid, Output output) throws IOException;

    boolean search(RemoteRepository remoteRepository, String expression, Output output) throws IOException;

    boolean verify(RemoteRepository remoteRepository, String gav, String sha1, Output output) throws IOException;
}
