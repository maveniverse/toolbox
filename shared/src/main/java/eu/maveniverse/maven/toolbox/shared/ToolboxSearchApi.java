/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.apache.maven.search.api.Record;
import org.apache.maven.search.api.SearchBackend;
import org.apache.maven.search.api.request.Query;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.slf4j.Logger;

/**
 * The Toolbox Search API implements "lower level" Search API related operations.
 */
public interface ToolboxSearchApi {
    Map<String, RemoteRepository> getKnownRemoteRepositories();

    SearchBackend getRemoteRepositoryBackend(RemoteRepository remoteRepository);

    SearchBackend getSmoBackend(RemoteRepository remoteRepository);

    void renderPage(List<Record> page, Predicate<String> versionPredicate, Logger output);

    boolean exists(SearchBackend backend, Artifact artifact) throws IOException;

    boolean verify(SearchBackend backend, Artifact artifact, String sha1) throws IOException;

    /**
     * Query out of {@link Artifact} for RR backend: it maps all that are given.
     */
    Query toRrQuery(Artifact artifact);

    /**
     * Query out of {@link Artifact} for SMO backend: SMO "have no idea" what file extension is, it handles only
     * "packaging", so we map here {@link Artifact#getExtension()} into "packaging" instead. Also, if we query
     * fields with value "*" SMO throws HTTP 400, so we simply omit "*" from queries, but they still allow us to
     * enter "*:*:1.0" that translates to "version=1.0" query.
     */
    Query toSmoQuery(Artifact artifact);
}
