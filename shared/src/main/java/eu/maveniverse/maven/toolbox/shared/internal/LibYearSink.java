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
import eu.maveniverse.maven.toolbox.shared.ArtifactSink;
import eu.maveniverse.maven.toolbox.shared.Output;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import org.apache.maven.search.api.SearchBackend;
import org.apache.maven.search.api.SearchRequest;
import org.apache.maven.search.api.SearchResponse;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.deployment.DeploymentException;
import org.eclipse.aether.repository.RemoteRepository;
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
        private final String currentVersion;
        private final Instant currentVersionInstant;
        private final String latestVersion;
        private final Instant latestVersionInstant;

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

        public String getCurrentVersion() {
            return currentVersion;
        }

        public Instant getCurrentVersionInstant() {
            return currentVersionInstant;
        }

        public String getLatestVersion() {
            return latestVersion;
        }

        public Instant getLatestVersionInstant() {
            return latestVersionInstant;
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
            BiFunction<Artifact, List<Version>, Version> versionSelector) {
        return new LibYearSink(
                output, context, toolboxResolver, toolboxSearchApi, quiet, allowSnapshots, versionSelector);
    }

    private final Output output;
    private final Context context;
    private final ToolboxResolverImpl toolboxResolver;
    private final ToolboxSearchApiImpl toolboxSearchApi;
    private final boolean quiet;
    private final boolean allowSnapshots;
    private final BiFunction<Artifact, List<Version>, Version> versionSelector;
    private final ConcurrentMap<Artifact, LibYear> libYear;

    private LibYearSink(
            Output output,
            Context context,
            ToolboxResolverImpl toolboxResolver,
            ToolboxSearchApiImpl toolboxSearchApi,
            boolean quiet,
            boolean allowSnapshots,
            BiFunction<Artifact, List<Version>, Version> versionSelector) {
        this.output = requireNonNull(output, "output");
        this.context = requireNonNull(context, "context");
        this.toolboxResolver = requireNonNull(toolboxResolver, "toolboxResolver");
        this.toolboxSearchApi = requireNonNull(toolboxSearchApi, "toolboxSearchApi");
        this.quiet = quiet;
        this.allowSnapshots = allowSnapshots;
        this.versionSelector = requireNonNull(versionSelector);
        this.libYear = new ConcurrentHashMap<>();
    }

    public Map<Artifact, LibYear> getLibYear() {
        return libYear;
    }

    @Override
    public void accept(Artifact artifact) throws IOException {
        requireNonNull(artifact, "artifact");
        libYear.computeIfAbsent(artifact, a -> {
            try {
                String currentVersion = artifact.getVersion();
                Instant currentVersionInstant = artifactPublishDate(artifact);
                String latestVersion = currentVersion;
                Instant latestVersionInstant = null;
                latestVersion = versionSelector
                        .apply(artifact, toolboxResolver.findNewerVersions(artifact, allowSnapshots))
                        .toString();
                latestVersionInstant = artifactPublishDate(artifact.setVersion(latestVersion));
                return new LibYear(currentVersion, currentVersionInstant, latestVersion, latestVersionInstant);
            } catch (IOException | VersionRangeResolutionException e) {
                // ignore
                return null;
            }
        });
    }

    @Override
    public void close() throws DeploymentException {
        float totalLibYears = 0;
        int outdated = 0;
        String indent = "-> ";
        if (!quiet) {
            output.normal("{}------------------------------", indent);
            for (Map.Entry<Artifact, LibYear> entry : getLibYear().entrySet()) {
                String line = "";
                if (entry.getValue() != null) {
                    LibYear value = entry.getValue();
                    if (Objects.equals(value.getCurrentVersion(), value.getLatestVersion())) {
                        continue;
                    }
                    outdated++;

                    LocalDate currentVersionDate = value.getCurrentVersionInstant() != null
                            ? value.getCurrentVersionInstant()
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                            : null;
                    LocalDate latestVersionDate = value.getLatestVersionInstant() != null
                            ? value.getLatestVersionInstant()
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                            : null;

                    if (currentVersionDate != null && latestVersionDate != null) {
                        long libWeeksOutdated = ChronoUnit.WEEKS.between(currentVersionDate, latestVersionDate);
                        float libYearsOutdated = libWeeksOutdated / 52f;
                        totalLibYears += libYearsOutdated;
                        line = String.format(
                                "%.2f years from %s %s (%s) => %s (%s)",
                                libYearsOutdated,
                                ArtifactIdUtils.toVersionlessId(entry.getKey()),
                                value.getCurrentVersion(),
                                currentVersionDate,
                                value.getLatestVersion(),
                                latestVersionDate);
                    } else {
                        line = String.format(
                                "%s %s (?) => %s (?)",
                                ArtifactIdUtils.toVersionlessId(entry.getKey()),
                                value.getCurrentVersion(),
                                value.getLatestVersion());
                    }
                } else {
                    line = entry.getKey() + " ?";
                }
                output.normal("{}{}", indent, line);
            }
        }
        output.normal("{}------------------------------", indent);
        output.normal(
                "{} Total of {} years from {} dependencies", indent, String.format("%.2f", totalLibYears), outdated);
        output.normal("{}------------------------------", indent);
    }

    private Instant artifactPublishDate(Artifact artifact) throws IOException {
        for (RemoteRepository remoteRepository : context.remoteRepositories()) {
            try (SearchBackend backend = toolboxSearchApi.getSmoBackend(remoteRepository)) {
                SearchRequest searchRequest = new SearchRequest(toolboxSearchApi.toSmoQuery(artifact));
                SearchResponse searchResponse = backend.search(searchRequest);
                if (searchResponse.getCurrentHits() > 0) {
                    Long lastUpdated =
                            searchResponse.getPage().iterator().next().getLastUpdated();
                    if (lastUpdated != null) {
                        return Instant.ofEpochMilli(lastUpdated);
                    }
                }
            } catch (IllegalArgumentException e) {
                // most likely not SMO service (remote repo is not CENTRAL); ignore
            }
        }
        return null;
    }
}
