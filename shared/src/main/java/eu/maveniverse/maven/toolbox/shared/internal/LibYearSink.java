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
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import org.apache.maven.search.api.SearchBackend;
import org.apache.maven.search.api.SearchRequest;
import org.apache.maven.search.api.SearchResponse;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.deployment.DeploymentException;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;
import org.eclipse.aether.version.Version;
import org.slf4j.Logger;

/**
 * Construction to calculate "libyear".
 *
 * @see <a href="https://libyear.com/">libyear</a>
 */
public final class LibYearSink implements Artifacts.Sink {
    public static final class LibYear {
        public final String currentVersion;
        public final Instant currentVersionInstant;
        public final List<Version> allVersions;
        public final String latestVersion;
        public final Instant latestVersionInstant;

        private LibYear(
                String currentVersion,
                Instant currentVersionInstant,
                List<Version> allVersions,
                String latestVersion,
                Instant latestVersionInstant) {
            this.currentVersion = currentVersion;
            this.currentVersionInstant = currentVersionInstant;
            this.allVersions = allVersions;
            this.latestVersion = latestVersion;
            this.latestVersionInstant = latestVersionInstant;
        }
    }

    /**
     * Creates libYear sink.
     */
    public static LibYearSink libYear(
            Logger output,
            String subject,
            Context context,
            ToolboxResolverImpl toolboxResolver,
            ToolboxSearchApiImpl toolboxSearchApi,
            boolean quiet,
            boolean upToDate,
            Predicate<Version> versionFilter,
            BiFunction<Artifact, List<Version>, String> versionSelector,
            List<SearchBackend> searchBackends) {
        return new LibYearSink(
                output,
                subject,
                context,
                toolboxResolver,
                toolboxSearchApi,
                quiet,
                upToDate,
                versionFilter,
                versionSelector,
                searchBackends);
    }

    private final Logger output;
    private final String subject;
    private final Context context;
    private final ToolboxResolverImpl toolboxResolver;
    private final ToolboxSearchApiImpl toolboxSearchApi;
    private final boolean quiet;
    private final boolean upToDate;
    private final Predicate<Version> versionFilter;
    private final BiFunction<Artifact, List<Version>, String> versionSelector;
    private final LocalDate now;
    private final CopyOnWriteArraySet<Artifact> artifacts;

    private final List<SearchBackend> searchBackends;

    private LibYearSink(
            Logger output,
            String subject,
            Context context,
            ToolboxResolverImpl toolboxResolver,
            ToolboxSearchApiImpl toolboxSearchApi,
            boolean quiet,
            boolean upToDate,
            Predicate<Version> versionFilter,
            BiFunction<Artifact, List<Version>, String> versionSelector,
            List<SearchBackend> searchBackends) {
        this.output = requireNonNull(output, "logger");
        this.subject = requireNonNull(subject, "subject");
        this.context = requireNonNull(context, "context");
        this.toolboxResolver = requireNonNull(toolboxResolver, "toolboxResolver");
        this.toolboxSearchApi = requireNonNull(toolboxSearchApi, "toolboxSearchApi");
        this.quiet = quiet;
        this.upToDate = upToDate;
        this.versionFilter = requireNonNull(versionFilter);
        this.versionSelector = requireNonNull(versionSelector);
        this.now = Instant.now().atZone(ZoneId.systemDefault()).toLocalDate();
        this.artifacts = new CopyOnWriteArraySet<>();
        this.searchBackends = requireNonNull(searchBackends);

        output.info("Calculating libYear for {}", subject);
    }

    @SuppressWarnings("unchecked")
    public ConcurrentMap<Artifact, LibYear> getLibYear() {
        ConcurrentMap<Artifact, LibYear> result = (ConcurrentMap<Artifact, LibYear>)
                context.repositorySystemSession().getData().get(LibYear.class);
        if (result == null) {
            result = new ConcurrentHashMap<>();
            context.repositorySystemSession().getData().set(LibYear.class, result);
        }
        return result;
    }

    @Override
    public void accept(Artifact artifact) throws IOException {
        requireNonNull(artifact, "artifact");
        getLibYear().computeIfAbsent(artifact, a -> {
            String currentVersion = artifact.getVersion();
            Instant currentVersionInstant = null;
            List<Version> allVersions = null;
            String latestVersion = currentVersion;
            Instant latestVersionInstant = null;
            try {
                currentVersionInstant = artifactPublishDate(artifact);
                allVersions = toolboxResolver.findNewerVersions(artifact, versionFilter);
                latestVersion = versionSelector.apply(artifact, allVersions);
                latestVersionInstant = Objects.equals(currentVersion, latestVersion)
                        ? currentVersionInstant
                        : artifactPublishDate(artifact.setVersion(latestVersion));
            } catch (VersionRangeResolutionException e) {
                // ignore
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return new LibYear(currentVersion, currentVersionInstant, allVersions, latestVersion, latestVersionInstant);
        });
        artifacts.add(artifact);
    }

    @Override
    public void close() throws DeploymentException {
        try {
            float totalLibYears = 0;
            TreeMap<Float, List<String>> outdatedWithLibyear = new TreeMap<>(Collections.reverseOrder());
            TreeSet<String> outdatedWithoutLibyear = new TreeSet<>();
            TreeSet<String> upToDateByAge = new TreeSet<>();
            if (!quiet) {
                for (Artifact artifact : artifacts) {
                    LibYear libYear = getLibYear().get(artifact);
                    if (libYear != null) {
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

                        if (Objects.equals(libYear.currentVersion, libYear.latestVersion)) {
                            long libCurrentWeeksAge =
                                    currentVersionDate != null ? ChronoUnit.WEEKS.between(currentVersionDate, now) : -1;
                            float libCurrentYearsAge = libCurrentWeeksAge != -1 ? libCurrentWeeksAge / 52f : -1;
                            if (libCurrentYearsAge != -1) {
                                upToDateByAge.add(String.format(
                                        "%s %s (%s) [latest is %.2f years old]",
                                        ArtifactIdUtils.toVersionlessId(artifact),
                                        libYear.currentVersion,
                                        currentVersionDate,
                                        libCurrentYearsAge));
                            } else {
                                upToDateByAge.add(String.format(
                                        "%s %s (%s) [latest age unknown]",
                                        ArtifactIdUtils.toVersionlessId(artifact),
                                        libYear.currentVersion,
                                        currentVersionDate == null ? "?" : currentVersionDate));
                            }
                        } else if (currentVersionDate != null && latestVersionDate != null) {
                            if (currentVersionDate.isAfter(latestVersionDate)) {
                                continue;
                            }
                            long libLatestWeeksAge = ChronoUnit.WEEKS.between(latestVersionDate, now);
                            long libWeeksOutdated = ChronoUnit.WEEKS.between(currentVersionDate, latestVersionDate);
                            if (libWeeksOutdated < 0) {
                                continue;
                            }
                            float libLatestYearsAge = libLatestWeeksAge / 52f;
                            float libYearsOutdated = libWeeksOutdated / 52f;
                            totalLibYears += libYearsOutdated;
                            outdatedWithLibyear
                                    .computeIfAbsent(libYearsOutdated, k -> new CopyOnWriteArrayList<>())
                                    .add(String.format(
                                            "%.2f years from %s %s (%s) => %s (%s) [latest is %.2f years old]",
                                            libYearsOutdated,
                                            ArtifactIdUtils.toVersionlessId(artifact),
                                            libYear.currentVersion,
                                            currentVersionDate,
                                            libYear.latestVersion,
                                            latestVersionDate,
                                            libLatestYearsAge));
                        } else {
                            outdatedWithoutLibyear.add(String.format(
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

            String indent = "  ";
            if (!outdatedWithLibyear.isEmpty()) {
                output.info("{}Outdated versions with known age", indent);
                outdatedWithLibyear.values().stream()
                        .flatMap(Collection::stream)
                        .forEach(l -> output.info("{}{}", indent, l));
                output.info("{}", indent);
            }
            if (!outdatedWithoutLibyear.isEmpty()) {
                output.info("{}Outdated versions without known age", indent);
                outdatedWithoutLibyear.forEach(l -> output.info("{}{}", indent, l));
                output.info("{}", indent);
            }
            if (upToDate) {
                if (!upToDateByAge.isEmpty()) {
                    output.info("{}Up to date versions", indent);
                }
                upToDateByAge.forEach(l -> output.info("{}{}", indent, l));
                output.info("{}", indent);
            }
            output.info(
                    "Total of {} years from {} outdated dependencies for {}",
                    String.format("%.2f", totalLibYears),
                    outdatedWithLibyear.values().stream().mapToInt(List::size).sum() + outdatedWithoutLibyear.size(),
                    subject);
            output.info("{}", indent);
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
                Long lastUpdated = searchResponse.getPage().getFirst().getLastUpdated();
                if (lastUpdated != null) {
                    return Instant.ofEpochMilli(lastUpdated);
                }
            }
        }
        return null;
    }
}
