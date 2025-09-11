/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static java.util.Objects.requireNonNull;
import static org.apache.maven.search.api.request.BooleanQuery.and;
import static org.apache.maven.search.api.request.FieldQuery.fieldQuery;

import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.toolbox.shared.ToolboxSearchApi;
import eu.maveniverse.maven.toolbox.shared.output.Output;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.ArtifactType;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.ConfigUtils;

public class ToolboxSearchApiImpl implements ToolboxSearchApi {
    protected final Output output;

    public ToolboxSearchApiImpl(Output output) {
        this.output = requireNonNull(output, "output");
    }

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
    @Override
    public SearchBackend getRemoteRepositoryBackend(
            RepositorySystemSession session, RemoteRepository remoteRepository, String repositoryVendor) {
        output.chatter(
                "Creating backend for {} (vendor={})",
                remoteRepository,
                repositoryVendor == null ? "n/a" : repositoryVendor);
        if (repositoryVendor == null) {
            repositoryVendor = (String) session.getConfigProperties().get("toolbox.search.backend.type");
            if (repositoryVendor == null) {
                if ("central".equals(remoteRepository.getContentType())) {
                    repositoryVendor = "central";
                } else if ("nx2".equals(remoteRepository.getContentType())) {
                    repositoryVendor = "nx2";
                } else {
                    // Some heuristics trying to figure out (probably not ideal)
                    if (ContextOverrides.CENTRAL.getId().equals(remoteRepository.getId())
                            && ContextOverrides.CENTRAL.getUrl().equals(remoteRepository.getUrl())) {
                        repositoryVendor = "central";
                    } else if (remoteRepository.getUrl().startsWith("https://repo.maven.apache.org/maven2")
                            || remoteRepository.getUrl().startsWith("https://repo1.maven.org/maven2/")) {
                        repositoryVendor = "central";
                    } else if (remoteRepository.getUrl().startsWith("https://repository.apache.org/")
                            || remoteRepository.getUrl().startsWith("https://oss.sonatype.org/")
                            || remoteRepository.getUrl().startsWith("https://s01.oss.sonatype.org/")) {
                        repositoryVendor = "nx2";
                    } else if (remoteRepository.getUrl().contains("/content/groups/")
                            || remoteRepository.getUrl().contains("/content/repositories/")) {
                        repositoryVendor = "nx2";
                    }
                }
            }
            output.chatter("Vendor guessed to {}", repositoryVendor);
        }
        final ResponseExtractor extractor;
        if ("central".equalsIgnoreCase(repositoryVendor)) {
            extractor = new MavenCentralResponseExtractor();
        } else if ("nx2".equalsIgnoreCase(repositoryVendor)) {
            extractor = new Nx2ResponseExtractor();
        } else {
            throw new IllegalArgumentException(
                    "Unsupported Search RR extractor: '" + repositoryVendor + "'; (supported are 'central', 'nx2')");
        }
        output.chatter(
                "Creating backend {}-rr {}:{}:{}",
                remoteRepository.getId(),
                remoteRepository.getId(),
                remoteRepository.getId(),
                repositoryVendor);
        return RemoteRepositorySearchBackendFactory.create(
                remoteRepository.getId() + "-rr",
                remoteRepository.getId(),
                remoteRepository.getUrl(),
                new Java11HttpClientTransport(
                        Java11HttpClientFactory.DEFAULT_TIMEOUT,
                        Java11HttpClientFactory.buildHttpClient(session, remoteRepository)),
                extractor);
    }

    /**
     * Creates SMO search backend: it works only for Maven Central, obviously.
     */
    @Override
    public SearchBackend getSmoBackend(RepositorySystemSession session, RemoteRepository remoteRepository) {
        if (!ContextOverrides.CENTRAL.getId().equals(remoteRepository.getId())) {
            throw new IllegalArgumentException("The SMO service is offered for Central only");
        }
        String backend =
                ConfigUtils.getString(session, SmoSearchBackendFactory.CSC_BACKEND_ID, "toolbox.search.smoBackend");
        if (SmoSearchBackendFactory.CSC_BACKEND_ID.equals(backend)) {
            output.chatter("Creating CSC backend");
            return SmoSearchBackendFactory.create(
                    SmoSearchBackendFactory.CSC_BACKEND_ID,
                    remoteRepository.getId(),
                    SmoSearchBackendFactory.CSC_SMO_URI,
                    new Java11HttpClientTransport(
                            Java11HttpClientFactory.DEFAULT_TIMEOUT,
                            Java11HttpClientFactory.buildHttpClient(session, remoteRepository)));
        } else if (SmoSearchBackendFactory.SMO_BACKEND_ID.equals(backend)) {
            output.chatter("Creating SMO backend");
            return SmoSearchBackendFactory.create(
                    SmoSearchBackendFactory.SMO_BACKEND_ID,
                    remoteRepository.getId(),
                    SmoSearchBackendFactory.SMO_SMO_URI,
                    new Java11HttpClientTransport(
                            Java11HttpClientFactory.DEFAULT_TIMEOUT,
                            Java11HttpClientFactory.buildHttpClient(session, remoteRepository)));
        } else {
            throw new IllegalArgumentException("Unknown SMO service backend: " + backend);
        }
    }

    @Override
    public List<String> renderGavoid(List<Record> page, Predicate<String> versionPredicate) {
        ArrayList<String> result = new ArrayList<>();
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
            if (record.hasField(MAVEN.VERSION)) {
                sb.append(":").append(record.getValue(MAVEN.VERSION));
            }

            result.add(sb.toString());
        }
        return result;
    }

    @Override
    public Collection<Artifact> renderArtifacts(
            RepositorySystemSession session, List<Record> page, Predicate<String> versionPredicate) {
        ArrayList<Artifact> result = new ArrayList<>();
        for (Record record : page) {
            final String version = record.getValue(MAVEN.VERSION);
            if (version != null && versionPredicate != null && !versionPredicate.test(version)) {
                continue;
            }

            final String groupId = record.getValue(MAVEN.GROUP_ID);
            final String artifactId = record.getValue(MAVEN.ARTIFACT_ID);
            final String classifier = record.getValue(MAVEN.CLASSIFIER);
            final String packaging = record.getValue(MAVEN.PACKAGING);
            String fileExtension = record.getValue(MAVEN.FILE_EXTENSION);

            if (fileExtension == null || fileExtension.trim().isEmpty() && packaging != null) {
                ArtifactType type = session.getArtifactTypeRegistry().get(packaging);
                if (type != null) {
                    fileExtension = type.getExtension();
                }
            }

            if (fileExtension == null || fileExtension.trim().isEmpty()) {
                continue;
            }

            HashMap<String, String> properties = new HashMap<>();
            if (packaging != null) {
                properties.put("packaging", packaging);
            }
            if (record.getLastUpdated() != null) {
                properties.put("lastUpdated", String.valueOf(record.getLastUpdated()));
            }
            if (record.hasField(MAVEN.VERSION_COUNT)) {
                properties.put("versionCount", String.valueOf(record.getValue(MAVEN.VERSION_COUNT)));
            }
            if (record.hasField(MAVEN.HAS_SOURCE)) {
                properties.put("hasSource", Boolean.toString(record.getValue(MAVEN.HAS_SOURCE)));
            }
            if (record.hasField(MAVEN.HAS_JAVADOC)) {
                properties.put("hasJavadoc", Boolean.toString(record.getValue(MAVEN.HAS_JAVADOC)));
            }

            result.add(new DefaultArtifact(groupId, artifactId, classifier, fileExtension, version)
                    .setProperties(properties));
        }
        return result;
    }

    @Override
    public boolean exists(SearchBackend backend, Artifact artifact) throws IOException {
        Query query = toRrQuery(artifact);
        SearchRequest searchRequest = new SearchRequest(query);
        SearchResponse searchResponse = backend.search(searchRequest);
        output.chatter(
                "SearchRequest: {} SearchResponse TH/CH {}/{}",
                searchResponse.getSearchRequest(),
                searchResponse.getTotalHits(),
                searchResponse.getCurrentHits());
        return searchResponse.getTotalHits() == 1;
    }

    @Override
    public boolean verify(SearchBackend backend, Artifact artifact, String sha1) throws IOException {
        Query query = toRrQuery(artifact);
        query = and(query, fieldQuery(MAVEN.SHA1, sha1));
        SearchRequest searchRequest = new SearchRequest(query);
        SearchResponse searchResponse = backend.search(searchRequest);
        output.chatter(
                "SearchRequest: {} SearchResponse TH/CH {}/{}",
                searchResponse.getSearchRequest(),
                searchResponse.getTotalHits(),
                searchResponse.getCurrentHits());
        return searchResponse.getTotalHits() == 1;
    }

    @Override
    public Map<String, Artifact> identify(
            RepositorySystemSession session, SearchBackend searchBackend, Collection<String> sha1s) throws IOException {
        HashMap<String, Artifact> result = new HashMap<>(sha1s.size());
        for (String sha1 : sha1s) {
            output.suggest("Identifying artifact with SHA1={}", sha1);
            SearchRequest searchRequest = new SearchRequest(fieldQuery(MAVEN.SHA1, sha1));
            SearchResponse searchResponse = searchBackend.search(searchRequest);
            output.chatter(
                    "SearchRequest: {} SearchResponse TH/CH {}/{}",
                    searchResponse.getSearchRequest(),
                    searchResponse.getTotalHits(),
                    searchResponse.getCurrentHits());
            if (searchResponse.getCurrentHits() == 0) {
                result.put(sha1, null);
            } else {
                while (searchResponse.getCurrentHits() > 0) {
                    Collection<Artifact> res = renderArtifacts(session, searchResponse.getPage(), null);
                    if (res.isEmpty()) {
                        result.put(sha1, null);
                    } else {
                        for (Artifact artifact : res) {
                            result.put(sha1, artifact);
                        }
                    }

                    searchResponse = searchBackend.search(
                            searchResponse.getSearchRequest().nextPage());
                    output.chatter(
                            "SearchRequest: {} SearchResponse TH/CH {}/{}",
                            searchResponse.getSearchRequest(),
                            searchResponse.getTotalHits(),
                            searchResponse.getCurrentHits());
                }
            }
        }
        return result;
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
