/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.toolbox.shared.ArtifactSink;
import eu.maveniverse.maven.toolbox.shared.Output;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.BiFunction;
import org.apache.maven.search.api.SearchBackend;
import org.apache.maven.search.api.SearchRequest;
import org.apache.maven.search.api.SearchResponse;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.deployment.DeploymentException;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;
import org.eclipse.aether.version.Version;

/**
 * Construction to calculate "libyear".
 *
 * @see <a href="https://libyear.com/">libyear</a>
 */
public final class LibYearSink implements ArtifactSink {
    public static final class LibYear {
        public final String currentVersion;
        public final Instant currentVersionInstant;
        public final String latestVersion;
        public final Instant latestVersionInstant;

        private LibYear(
                String currentVersion,
                Instant currentVersionInstant,
                String latestVersion,
                Instant latestVersionInstant) {
            this.currentVersion = currentVersion;
            this.currentVersionInstant = currentVersionInstant;
            this.latestVersion = latestVersion;
            this.latestVersionInstant = latestVersionInstant;
        }
    }

    /**
     * Creates libYear sink.
     */
    public static LibYearSink libYear(
            Output output,
            Context context,
            ToolboxResolverImpl toolboxResolver,
            ToolboxSearchApiImpl toolboxSearchApi,
            boolean quiet,
            boolean allowSnapshots,
            BiFunction<Artifact, List<Version>, String> versionSelector) {
        return new LibYearSink(
                output, context, toolboxResolver, toolboxSearchApi, quiet, allowSnapshots, versionSelector);
    }

    private final Output output;
    private final Context context;
    private final ToolboxResolverImpl toolboxResolver;
    private final ToolboxSearchApiImpl toolboxSearchApi;
    private final boolean quiet;
    private final boolean allowSnapshots;
    private final BiFunction<Artifact, List<Version>, String> versionSelector;
    private final CopyOnWriteArraySet<Artifact> artifacts;

    private final List<SearchBackend> searchBackends;

    private LibYearSink(
            Output output,
            Context context,
            ToolboxResolverImpl toolboxResolver,
            ToolboxSearchApiImpl toolboxSearchApi,
            boolean quiet,
            boolean allowSnapshots,
            BiFunction<Artifact, List<Version>, String> versionSelector) {
        this.output = requireNonNull(output, "output");
        this.context = requireNonNull(context, "context");
        this.toolboxResolver = requireNonNull(toolboxResolver, "toolboxResolver");
        this.toolboxSearchApi = requireNonNull(toolboxSearchApi, "toolboxSearchApi");
        this.quiet = quiet;
        this.allowSnapshots = allowSnapshots;
        this.versionSelector = requireNonNull(versionSelector);
        this.artifacts = new CopyOnWriteArraySet<>();

        this.searchBackends = new ArrayList<>();
        // hack: using central only to see it in action
        this.searchBackends.add(toolboxSearchApi.getRemoteRepositoryBackend(toolboxResolver.parseRemoteRepository(
                ContextOverrides.CENTRAL.getId() + "::central::" + ContextOverrides.CENTRAL.getUrl())));
        //        for (RemoteRepository remoteRepository : context.remoteRepositories()) {
        //            try {
        //                this.searchBackends.add(toolboxSearchApi.getRemoteRepositoryBackend(remoteRepository));
        //            } catch (IllegalArgumentException e) {
        //                // most likely not SMO service (remote repo is not CENTRAL); ignore
        //            }
        //        }
    }

    @SuppressWarnings("unchecked")
    public ConcurrentMap<Artifact, LibYear> getLibYear() {
        return (ConcurrentMap<Artifact, LibYear>)
                context.repositorySystemSession().getData().computeIfAbsent(LibYear.class, ConcurrentHashMap::new);
    }

    @Override
    public void accept(Artifact artifact) throws IOException {
        requireNonNull(artifact, "artifact");
        getLibYear().computeIfAbsent(artifact, a -> {
            String currentVersion = artifact.getVersion();
            Instant currentVersionInstant = null;
            String latestVersion = currentVersion;
            Instant latestVersionInstant = null;
            try {
                currentVersionInstant = artifactPublishDate(artifact);
                latestVersion =
                        versionSelector.apply(artifact, toolboxResolver.findNewerVersions(artifact, allowSnapshots));
                latestVersionInstant = artifactPublishDate(artifact.setVersion(latestVersion));
            } catch (VersionRangeResolutionException e) {
                // ignore
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return new LibYear(currentVersion, currentVersionInstant, latestVersion, latestVersionInstant);
        });
        artifacts.add(artifact);
    }

    @Override
    public void close() throws DeploymentException {
        try {
            float totalLibYears = 0;
            TreeMap<Float, List<String>> withLibyear = new TreeMap<>(Collections.reverseOrder());
            TreeSet<String> withoutLibyear = new TreeSet<>();
            if (!quiet) {
                for (Artifact artifact : artifacts) {
                    LibYear libYear = getLibYear().get(artifact);
                    if (libYear != null) {
                        if (Objects.equals(libYear.currentVersion, libYear.latestVersion)) {
                            continue;
                        }

                        LocalDate currentVersionDate = libYear.currentVersionInstant != null
                                ? libYear.currentVersionInstant
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDate()
                                : null;
                        LocalDate latestVersionDate = libYear.latestVersionInstant != null
                                ? libYear.latestVersionInstant
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDate()
                                : null;

                        if (currentVersionDate != null && latestVersionDate != null) {
                            long libWeeksOutdated = ChronoUnit.WEEKS.between(currentVersionDate, latestVersionDate);
                            if (libWeeksOutdated < 0) {
                                continue;
                            }
                            float libYearsOutdated = libWeeksOutdated / 52f;
                            totalLibYears += libYearsOutdated;
                            withLibyear
                                    .computeIfAbsent(libYearsOutdated, k -> new CopyOnWriteArrayList<>())
                                    .add(String.format(
                                            "%.2f years from %s %s (%s) => %s (%s)",
                                            libYearsOutdated,
                                            ArtifactIdUtils.toVersionlessId(artifact),
                                            libYear.currentVersion,
                                            currentVersionDate,
                                            libYear.latestVersion,
                                            latestVersionDate));
                        } else {
                            withoutLibyear.add(String.format(
                                    "%s %s (%s) => %s (%s)",
                                    ArtifactIdUtils.toVersionlessId(artifact),
                                    libYear.currentVersion,
                                    currentVersionDate == null ? "?" : currentVersionDate,
                                    libYear.latestVersion,
                                    latestVersionDate == null ? "?" : latestVersionDate));
                        }
                    }
                }
            }

            String indent = "";
            if (!withLibyear.isEmpty()) {
                output.normal("{}Outdated versions with known age", indent);
            }
            withLibyear.values().stream().flatMap(Collection::stream).forEach(l -> output.normal("{}{}", indent, l));
            output.normal("{}", indent);
            if (!withoutLibyear.isEmpty()) {
                output.normal("{}Outdated versions", indent);
            }
            withoutLibyear.forEach(l -> output.normal("{}{}", indent, l));
            output.normal("{}", indent);
            output.normal(
                    "{}Total of {} years from {} outdated dependencies",
                    indent,
                    String.format("%.2f", totalLibYears),
                    withLibyear.size() + withoutLibyear.size());
        } finally {
            searchBackends.forEach(b -> {
                try {
                    b.close();
                } catch (Exception e) {
                    output.warn("Could not close SearchBackend", e);
                }
            });
        }
    }

    private Instant artifactPublishDate(Artifact artifact) throws IOException {
        for (SearchBackend backend : searchBackends) {
            SearchRequest searchRequest = new SearchRequest(toolboxSearchApi.toRrQuery(artifact));
            SearchResponse searchResponse = backend.search(searchRequest);
            if (searchResponse.getCurrentHits() > 0) {
                Long lastUpdated = searchResponse.getPage().iterator().next().getLastUpdated();
                if (lastUpdated != null) {
                    return Instant.ofEpochMilli(lastUpdated);
                }
            }
        }
        return null;
    }
}
