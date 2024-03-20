/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static org.apache.maven.search.api.request.BooleanQuery.and;
import static org.apache.maven.search.api.request.FieldQuery.fieldQuery;

import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.toolbox.shared.ToolboxSearchApi;
import java.io.Closeable;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.apache.maven.search.api.MAVEN;
import org.apache.maven.search.api.Record;
import org.apache.maven.search.api.SearchBackend;
import org.apache.maven.search.api.SearchRequest;
import org.apache.maven.search.api.SearchResponse;
import org.apache.maven.search.api.request.Query;
import org.apache.maven.search.api.transport.Java11HttpClientTransport;
import org.apache.maven.search.backend.remoterepository.RemoteRepositorySearchBackendFactory;
import org.apache.maven.search.backend.remoterepository.ResponseExtractor;
import org.apache.maven.search.backend.remoterepository.extractor.MavenCentralResponseExtractor;
import org.apache.maven.search.backend.remoterepository.extractor.Nx2ResponseExtractor;
import org.apache.maven.search.backend.smo.SmoSearchBackendFactory;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToolboxSearchApiImpl implements ToolboxSearchApi, Closeable {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ConcurrentHashMap<String, SearchBackend> searchBackends;
    private final Map<String, RemoteRepository> knownRemoteRepositories;

    public ToolboxSearchApiImpl() {
        this.searchBackends = new ConcurrentHashMap<>();

        Map<String, RemoteRepository> rr = new HashMap<>();
        rr.put(ContextOverrides.CENTRAL.getId(), ContextOverrides.CENTRAL);
        rr.put(
                "sonatype-oss-releases",
                new RemoteRepository.Builder(
                                "sonatype-oss-releases",
                                "default",
                                "https://oss.sonatype.org/content/repositories/releases/")
                        .build());
        rr.put(
                "sonatype-oss-staging",
                new RemoteRepository.Builder(
                                "sonatype-oss-staging", "default", "https://oss.sonatype.org/content/groups/staging//")
                        .build());
        rr.put(
                "sonatype-s01-releases",
                new RemoteRepository.Builder(
                                "sonatype-s01-releases",
                                "default",
                                "https://s01.oss.sonatype.org/content/repositories/releases/")
                        .build());
        rr.put(
                "sonatype-s01-staging",
                new RemoteRepository.Builder(
                                "sonatype-s01-staging",
                                "default",
                                "https://s01.oss.sonatype.org/content/groups/staging//")
                        .build());
        rr.put(
                "apache-releases",
                new RemoteRepository.Builder(
                                "apache-releases",
                                "default",
                                "https://repository.apache.org/content/repositories/releases/")
                        .build());
        rr.put(
                "apache-staging",
                new RemoteRepository.Builder(
                                "apache-staging", "default", "https://repository.apache.org/content/groups/staging/")
                        .build());
        rr.put(
                "apache-maven-staging",
                new RemoteRepository.Builder(
                                "apache-maven-staging",
                                "default",
                                "https://repository.apache.org/content/groups/maven-staging-group/")
                        .build());
        this.knownRemoteRepositories = Collections.unmodifiableMap(rr);
    }

    @Override
    public void close() {
        searchBackends.forEach((key, value) -> {
            try {
                value.close();
            } catch (IOException ex) {
                // log+ignore
                logger.warn("Could not close search backend: {}", key, ex);
            }
        });
    }

    private SearchBackend getOrCreate(String key, Supplier<SearchBackend> supplier) {
        return searchBackends.computeIfAbsent(key, k -> supplier.get());
    }

    @Override
    public Map<String, RemoteRepository> getKnownRemoteRepositories() {
        return knownRemoteRepositories;
    }

    @Override
    public SearchBackend getRemoteRepositoryBackend(RemoteRepository remoteRepository) {
        return getOrCreate(SearchBackend.class.getName() + "-" + remoteRepository.getId(), () -> {
            final ResponseExtractor extractor;
            // TODO: this needs more
            if (ContextOverrides.CENTRAL.getId().equals(remoteRepository.getId())) {
                extractor = new MavenCentralResponseExtractor();
            } else {
                extractor = new Nx2ResponseExtractor();
            }
            return RemoteRepositorySearchBackendFactory.create(
                    remoteRepository.getId() + "-rr",
                    remoteRepository.getId(),
                    remoteRepository.getUrl(),
                    new Java11HttpClientTransport(),
                    extractor);
        });
    }

    @Override
    public SearchBackend getSmoBackend(RemoteRepository remoteRepository) {
        if (!ContextOverrides.CENTRAL.getId().equals(remoteRepository.getId())) {
            throw new IllegalArgumentException("The SMO service is offered for Central only");
        }
        return getOrCreate(
                remoteRepository.getId() + "-smo",
                () -> SmoSearchBackendFactory.create(
                        remoteRepository.getId() + "-smo",
                        remoteRepository.getId(),
                        "https://search.maven.org/solrsearch/select",
                        new Java11HttpClientTransport()));
    }

    @Override
    public void renderPage(List<Record> page, Predicate<String> versionPredicate, Logger output) {
        for (Record record : page) {
            final String version = record.getValue(MAVEN.VERSION);
            if (version != null && versionPredicate != null && !versionPredicate.test(version)) {
                continue;
            }
            StringBuilder sb = new StringBuilder();
            sb.append(record.getValue(MAVEN.GROUP_ID));
            if (record.hasField(MAVEN.ARTIFACT_ID)) {
                sb.append(":").append(record.getValue(MAVEN.ARTIFACT_ID));
            }
            if (record.hasField(MAVEN.VERSION)) {
                sb.append(":").append(record.getValue(MAVEN.VERSION));
            }
            if (record.hasField(MAVEN.PACKAGING)) {
                if (record.hasField(MAVEN.CLASSIFIER)) {
                    sb.append(":").append(record.getValue(MAVEN.CLASSIFIER));
                }
                sb.append(":").append(record.getValue(MAVEN.PACKAGING));
            } else if (record.hasField(MAVEN.FILE_EXTENSION)) {
                if (record.hasField(MAVEN.CLASSIFIER)) {
                    sb.append(":").append(record.getValue(MAVEN.CLASSIFIER));
                }
                sb.append(":").append(record.getValue(MAVEN.FILE_EXTENSION));
            }

            List<String> remarks = new ArrayList<>();
            if (record.getLastUpdated() != null) {
                remarks.add("lastUpdate=" + Instant.ofEpochMilli(record.getLastUpdated()));
            }
            if (record.hasField(MAVEN.VERSION_COUNT)) {
                remarks.add("versionCount=" + record.getValue(MAVEN.VERSION_COUNT));
            }
            if (record.hasField(MAVEN.HAS_SOURCE)) {
                remarks.add("hasSource=" + record.getValue(MAVEN.HAS_SOURCE));
            }
            if (record.hasField(MAVEN.HAS_JAVADOC)) {
                remarks.add("hasJavadoc=" + record.getValue(MAVEN.HAS_JAVADOC));
            }

            output.info(sb.toString());
            if (output.isDebugEnabled() && !remarks.isEmpty()) {
                output.info("   " + remarks);
            }
        }
    }

    @Override
    public boolean exists(SearchBackend backend, Artifact artifact) throws IOException {
        Query query = toRrQuery(artifact);
        SearchRequest searchRequest = new SearchRequest(query);
        SearchResponse searchResponse = backend.search(searchRequest);
        return searchResponse.getTotalHits() == 1;
    }

    @Override
    public boolean verify(SearchBackend backend, Artifact artifact, String sha1) throws IOException {
        Query query = toRrQuery(artifact);
        query = and(query, fieldQuery(MAVEN.SHA1, sha1));
        SearchRequest searchRequest = new SearchRequest(query);
        SearchResponse searchResponse = backend.search(searchRequest);
        return searchResponse.getTotalHits() == 1;
    }

    @Override
    public Query toRrQuery(Artifact artifact) {
        Query result = fieldQuery(MAVEN.GROUP_ID, artifact.getGroupId());
        result = and(result, fieldQuery(MAVEN.ARTIFACT_ID, artifact.getArtifactId()));
        result = and(result, fieldQuery(MAVEN.VERSION, artifact.getVersion()));
        if (artifact.getClassifier() != null && !artifact.getClassifier().isEmpty()) {
            result = and(result, fieldQuery(MAVEN.CLASSIFIER, artifact.getClassifier()));
        }
        return and(result, fieldQuery(MAVEN.FILE_EXTENSION, artifact.getExtension()));
    }

    @Override
    public Query toSmoQuery(Artifact artifact) {
        Query result = null;
        if (!"*".equals(artifact.getGroupId())) {
            result = fieldQuery(MAVEN.GROUP_ID, artifact.getGroupId());
        }
        if (!"*".equals(artifact.getArtifactId())) {
            result = result != null
                    ? and(result, fieldQuery(MAVEN.ARTIFACT_ID, artifact.getArtifactId()))
                    : fieldQuery(MAVEN.ARTIFACT_ID, artifact.getArtifactId());
        }
        if (!"*".equals(artifact.getVersion())) {
            result = result != null
                    ? and(result, fieldQuery(MAVEN.VERSION, artifact.getVersion()))
                    : fieldQuery(MAVEN.VERSION, artifact.getVersion());
        }
        if (!"*".equals(artifact.getClassifier()) && !"".equals(artifact.getClassifier())) {
            result = result != null
                    ? and(result, fieldQuery(MAVEN.CLASSIFIER, artifact.getClassifier()))
                    : fieldQuery(MAVEN.CLASSIFIER, artifact.getClassifier());
        }
        if (!"*".equals(artifact.getExtension())) {
            result = result != null
                    ? and(result, fieldQuery(MAVEN.PACKAGING, artifact.getExtension()))
                    : fieldQuery(MAVEN.PACKAGING, artifact.getExtension());
        }

        if (result == null) {
            throw new IllegalArgumentException("Too broad query expression");
        }
        return result;
    }
}
