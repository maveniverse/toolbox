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
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;

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

    Result<String> dump(Output output);

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

    /**
     * Calculates the classpath (returned string is OS FS specific) of given scope and root.
     */
    Result<String> classpath(ResolutionScope resolutionScope, ResolutionRoot resolutionRoot, Output output)
            throws Exception;

    /**
     * Returns the list of artifacts copied from source to sink.
     */
    Result<List<Artifact>> copy(Source<Artifact> source, Sink<Artifact> sink, Output output) throws Exception;

    /**
     * Returns the list of artifacts copied from transitively resolving given roots to sink.
     */
    Result<List<Artifact>> copyTransitive(
            ResolutionScope resolutionScope,
            Collection<ResolutionRoot> resolutionRoots,
            Sink<Artifact> sink,
            Output output)
            throws Exception;

    /**
     * Returns the list of artifacts copied from recorder to sink.
     */
    Result<List<Artifact>> copyRecorded(boolean stopRecording, Sink<Artifact> sink, Output output) throws Exception;

    /**
     * List repositories used to transitively resolve given root.
     */
    default Result<List<RemoteRepository>> listRepositories(
            ResolutionScope resolutionScope, String context, ResolutionRoot resolutionRoot, Output output)
            throws Exception {
        HashMap<String, ResolutionRoot> resolutionRoots = new HashMap<>();
        resolutionRoots.put(context, resolutionRoot);
        Result<Map<String, List<RemoteRepository>>> result = listRepositories(resolutionScope, resolutionRoots, output);
        return result.isSuccess()
                ? Result.success(result.getData().orElseThrow().get(context))
                : Result.failure(result.getMessage());
    }

    /**
     * List repositories used to transitively resolve given root.
     */
    Result<Map<String, List<RemoteRepository>>> listRepositories(
            ResolutionScope resolutionScope, Map<String, ResolutionRoot> resolutionRoots, Output output)
            throws Exception;

    /**
     * Lists available plugins in given groupId.
     */
    Result<List<Artifact>> listAvailablePlugins(Collection<String> groupIds, Output output) throws Exception;

    /**
     * Starts the recorder.
     */
    Result<String> recordStart(Output output);

    final class RecordStats {
        public final boolean active;
        public final int recordedCount;

        public RecordStats(boolean active, int recordedCount) {
            this.active = active;
            this.recordedCount = recordedCount;
        }
    }

    /**
     * Recorder stats.
     */
    Result<RecordStats> recordStats(Output output);

    /**
     * Stops the recorder.
     */
    Result<String> recordStop(Output output);

    /**
     * Resolves given artifacts.
     */
    Result<List<Artifact>> resolve(
            Source<Artifact> artifacts,
            boolean sources,
            boolean javadoc,
            boolean signature,
            Sink<Artifact> sink,
            Output output)
            throws Exception;

    /**
     * Resolves transitively given root.
     */
    Result<List<Artifact>> resolveTransitive(
            ResolutionScope resolutionScope,
            Collection<ResolutionRoot> resolutionRoots,
            boolean sources,
            boolean javadoc,
            boolean signature,
            Sink<Artifact> sink,
            Output output)
            throws Exception;

    /**
     * Returns the tree of root.
     */
    Result<CollectResult> tree(
            ResolutionScope resolutionScope, ResolutionRoot resolutionRoot, boolean verboseTree, Output output)
            throws Exception;

    /**
     * Creates tree for given root and searches for artifacts using matcher, returns list of paths to hits.
     */
    Result<List<List<Artifact>>> treeFind(
            ResolutionScope resolutionScope,
            ResolutionRoot resolutionRoot,
            boolean verboseTree,
            ArtifactMatcher artifactMatcher,
            Output output)
            throws Exception;

    /**
     * Returns the depMgt list of given root.
     */
    Result<List<Dependency>> dmList(ResolutionRoot resolutionRoot, boolean verboseList, Output output) throws Exception;

    /**
     * Returns the depMgt tree of given root.
     */
    Result<CollectResult> dmTree(ResolutionRoot resolutionRoot, boolean verboseTree, Output output) throws Exception;

    // Search API related commands: they target one single RemoteRepository

    /**
     * Returns a list of known <em>search remote repositories</em>. These {@link RemoteRepository} are NOT usable with
     * Resolver (i.e. to deploy to them), only with Search API!
     */
    Map<String, RemoteRepository> getKnownSearchRemoteRepositories();

    /**
     * Checks existence of GAV (and sub-artifacts optionally).
     */
    Result<Map<Artifact, Boolean>> exists(
            RemoteRepository remoteRepository,
            String gav,
            boolean pom,
            boolean sources,
            boolean javadoc,
            boolean signature,
            boolean allRequired,
            String repositoryVendor,
            Output output)
            throws IOException;

    /**
     * Identifies targets (a file or sha1) and returns matched artifacts.
     */
    Result<Map<String, Artifact>> identify(
            RemoteRepository remoteRepository, Collection<String> targets, boolean decorated, Output output)
            throws IOException;

    /**
     * Lists given "gavoid" and returns list of "gavoids".
     */
    Result<List<String>> list(RemoteRepository remoteRepository, String gavoid, String repositoryVendor, Output output)
            throws IOException;

    /**
     * Searches for artifacts.
     */
    Result<List<Artifact>> search(RemoteRepository remoteRepository, String expression, Output output)
            throws IOException;

    /**
     * Verifies artifact against given SHA-1.
     */
    Result<Boolean> verify(
            RemoteRepository remoteRepository, String gav, String sha1, String repositoryVendor, Output output)
            throws IOException;

    // Various

    /**
     * Calculates libyear with given parameters.
     */
    Result<Float> libYear(
            String subject,
            ResolutionScope resolutionScope,
            ResolutionRoot resolutionRoot,
            boolean transitive,
            boolean quiet,
            boolean upToDate,
            Predicate<Version> versionPredicate,
            BiFunction<Artifact, List<Version>, String> artifactVersionSelector,
            String repositoryVendor,
            Output output)
            throws Exception;

    /**
     * Finds newer versions for artifacts from source.
     */
    Result<Map<Artifact, List<Version>>> versions(
            String context, Source<Artifact> artifacts, Predicate<Version> versionPredicate, Output output)
            throws Exception;
}
