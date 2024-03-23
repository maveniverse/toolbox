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
import eu.maveniverse.maven.toolbox.shared.Output;
import eu.maveniverse.maven.toolbox.shared.ToolboxSearchApi;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
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

public class ToolboxSearchApiImpl implements ToolboxSearchApi {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public ToolboxSearchApiImpl() {}

    @Override
    public SearchBackend getRemoteRepositoryBackend(RemoteRepository remoteRepository) {
        final ResponseExtractor extractor;
        if ("central".equals(remoteRepository.getContentType())) {
            extractor = new MavenCentralResponseExtractor();
        } else if ("nx2".equals(remoteRepository.getContentType())) {
            extractor = new Nx2ResponseExtractor();
        } else {
            throw new IllegalArgumentException("Unsupported extractor");
        }
        return RemoteRepositorySearchBackendFactory.create(
                remoteRepository.getId() + "-rr",
                remoteRepository.getId(),
                remoteRepository.getUrl(),
                new Java11HttpClientTransport(),
                extractor);
    }

    @Override
    public SearchBackend getSmoBackend(RemoteRepository remoteRepository) {
        if (!ContextOverrides.CENTRAL.getId().equals(remoteRepository.getId())) {
            throw new IllegalArgumentException("The SMO service is offered for Central only");
        }
        return SmoSearchBackendFactory.create(
                remoteRepository.getId() + "-smo",
                remoteRepository.getId(),
                "https://search.maven.org/solrsearch/select",
                new Java11HttpClientTransport());
    }

    @Override
    public void renderPage(List<Record> page, Predicate<String> versionPredicate, Output output) {
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

            output.normal(sb.toString());
            if (output.isVerbose() && !remarks.isEmpty()) {
                output.verbose("   " + remarks);
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
