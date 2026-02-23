/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.apache.maven.search.api.Record;
import org.apache.maven.search.api.SearchBackend;
import org.apache.maven.search.api.request.Query;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;

public interface ToolboxSearchApi {
    /**
     * Creates RR search backend: it may be explicitly selected by using parameter {@code repositoryVendor}. The logic:
     * <ul>
     *     <li>if {@code repositoryVendor} is non-{@code null}, use it</li>
     *     <li>check {@code toolbox.search.backend.type} session config property</li>
     *     <li>check {@link RemoteRepository#getContentType()}</li>
     *     <li>finally, if none above, try some "heuristics"</li>
     * </ul>
     * This is all about the Search API RR backend extractor selection. Note: in some use cases "extractor" is not
     * used, so forcing any value in those cases is perfectly fine.
     *
     * @param session The session, must not be {@code null}.
     * @param remoteRepository The repository to create RR backend for, must not be {@code null}.
     * @param repositoryVendor The "override" type, or may be {@code null}, in which case logic above will be applied.
     *                         Basically determines extractor to be used with it.
     */
    SearchBackend getRemoteRepositoryBackend(
            RepositorySystemSession session, RemoteRepository remoteRepository, String repositoryVendor);

    /**
     * Creates SMO search backend: it works only for Maven Central, obviously.
     */
    SearchBackend getSmoBackend(RepositorySystemSession session, RemoteRepository remoteRepository);

    List<String> renderGavoid(List<Record> page, Predicate<String> versionPredicate);

    Collection<Artifact> renderArtifacts(
            RepositorySystemSession session, List<Record> page, Predicate<String> versionPredicate);

    boolean exists(SearchBackend backend, Artifact artifact) throws IOException;

    boolean verify(SearchBackend backend, Artifact artifact, String sha1) throws IOException;

    Map<String, Artifact> identify(
            RepositorySystemSession session, SearchBackend searchBackend, Collection<String> sha1s) throws IOException;

    Query toRrQuery(Artifact artifact);

    Query toSmoQuery(Artifact artifact);
}
