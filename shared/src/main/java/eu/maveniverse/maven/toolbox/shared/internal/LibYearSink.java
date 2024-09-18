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
import eu.maveniverse.maven.toolbox.shared.output.Output;
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
import java.util.concurrent.atomic.DoubleAdder;
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

/**
 * Construction to calculate "libyear".
 *
 * @see <a href="https://libyear.com/">libyear</a>
 */
public final class LibYearSink implements Artifacts.Sink {
    public static final class LibYear {
        public static final float UNKNOWN_AGE = -1f;

        public final String currentVersion;
        public final Instant currentVersionInstant;
        public final LocalDate currentVersionDate;
        public final List<Version> allVersions;
        public final String latestVersion;
        public final Instant latestVersionInstant;
        public final LocalDate latestVersionDate;

        private LibYear(
                String currentVersion,
                Instant currentVersionInstant,
                List<Version> allVersions,
                String latestVersion,
                Instant latestVersionInstant) {
            this.currentVersion = currentVersion;
            this.currentVersionInstant = currentVersionInstant;
            if (currentVersionInstant != null) {
                this.currentVersionDate =
                        currentVersionInstant.atZone(ZoneId.systemDefault()).toLocalDate();
            } else {
                this.currentVersionDate = null;
            }
            this.allVersions = List.copyOf(allVersions);
            this.latestVersion = latestVersion;
            this.latestVersionInstant = latestVersionInstant;
            if (latestVersionInstant != null) {
                this.latestVersionDate =
                        latestVersionInstant.atZone(ZoneId.systemDefault()).toLocalDate();
            } else {
                this.latestVersionDate = null;
            }
        }

        public boolean isUpToDate() {
            return Objects.equals(currentVersion, latestVersion);
        }

        public float yearsBetween(LocalDate from, LocalDate to) {
            if (from != null && to != null) {
                return ChronoUnit.WEEKS.between(from, to) / 52f;
            }
            return UNKNOWN_AGE;
        }
    }

    /**
     * Creates libYear sink.
     */
    public static LibYearSink libYear(
            Output output,
            String subject,
            Context context,
            ToolboxResolverImpl toolboxResolver,
            ToolboxSearchApiImpl toolboxSearchApi,
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
                upToDate,
                versionFilter,
                versionSelector,
                searchBackends);
    }

    private final Output output;
    private final String subject;
    private final Context context;
    private final ToolboxResolverImpl toolboxResolver;
    private final ToolboxSearchApiImpl toolboxSearchApi;
    private final boolean upToDate;
    private final Predicate<Version> versionFilter;
    private final BiFunction<Artifact, List<Version>, String> versionSelector;
    private final LocalDate now;
    private final CopyOnWriteArraySet<Artifact> artifacts;
    private final DoubleAdder totalLibyearAdder;

    private final List<SearchBackend> searchBackends;

    private LibYearSink(
            Output output,
            String subject,
            Context context,
            ToolboxResolverImpl toolboxResolver,
            ToolboxSearchApiImpl toolboxSearchApi,
            boolean upToDate,
            Predicate<Version> versionFilter,
            BiFunction<Artifact, List<Version>, String> versionSelector,
            List<SearchBackend> searchBackends) {
        this.output = requireNonNull(output, "logger");
        this.subject = requireNonNull(subject, "subject");
        this.context = requireNonNull(context, "context");
        this.toolboxResolver = requireNonNull(toolboxResolver, "toolboxResolver");
        this.toolboxSearchApi = requireNonNull(toolboxSearchApi, "toolboxSearchApi");
        this.upToDate = upToDate;
        this.versionFilter = requireNonNull(versionFilter);
        this.versionSelector = requireNonNull(versionSelector);
        this.now = Instant.now().atZone(ZoneId.systemDefault()).toLocalDate();
        this.artifacts = new CopyOnWriteArraySet<>();
        this.totalLibyearAdder = new DoubleAdder();
        this.searchBackends = requireNonNull(searchBackends);
    }

    public float getTotalLibyear() {
        return totalLibyearAdder.floatValue();
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
            output.chatter("Accepted and calculating libYear for {}", artifact);
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
            TreeMap<Float, List<String>> outdatedWithLibyear = new TreeMap<>(Collections.reverseOrder());
            TreeSet<String> outdatedWithoutLibyear = new TreeSet<>();
            TreeSet<String> upToDateByAge = new TreeSet<>();
            for (Artifact artifact : artifacts) {
                LibYear libYear = getLibYear().get(artifact);
                if (libYear != null) {
                    if (libYear.isUpToDate()) {
                        float libCurrentYearsAge = libYear.yearsBetween(libYear.currentVersionDate, now);
                        if (libCurrentYearsAge != LibYear.UNKNOWN_AGE) {
                            upToDateByAge.add(String.format(
                                    "%s %s (%s) [latest is %.2f years old]",
                                    ArtifactIdUtils.toVersionlessId(artifact),
                                    libYear.currentVersion,
                                    libYear.currentVersionDate,
                                    libCurrentYearsAge));
                        } else {
                            upToDateByAge.add(String.format(
                                    "%s %s (%s) [latest age unknown]",
                                    ArtifactIdUtils.toVersionlessId(artifact),
                                    libYear.currentVersion,
                                    libYear.currentVersionDate == null ? "?" : libYear.currentVersionDate));
                        }
                    } else {
                        float libCurrentYearsAge =
                                libYear.yearsBetween(libYear.currentVersionDate, libYear.latestVersionDate);
                        float libLatestYearsAge = libYear.yearsBetween(libYear.latestVersionDate, now);
                        if (libCurrentYearsAge != LibYear.UNKNOWN_AGE
                                && !libYear.currentVersionDate.isAfter(libYear.latestVersionDate)) {
                            // note: here we know that both 'current' and 'latest' are not null,
                            // hence second calc must work as well, as 'now' is never null
                            totalLibyearAdder.add(libCurrentYearsAge);
                            outdatedWithLibyear
                                    .computeIfAbsent(libCurrentYearsAge, k -> new CopyOnWriteArrayList<>())
                                    .add(String.format(
                                            "%.2f years from %s %s (%s) => %s (%s) [latest is %.2f years old]",
                                            libCurrentYearsAge,
                                            ArtifactIdUtils.toVersionlessId(artifact),
                                            libYear.currentVersion,
                                            libYear.currentVersionDate,
                                            libYear.latestVersion,
                                            libYear.latestVersionDate,
                                            libLatestYearsAge));
                        } else {
                            outdatedWithoutLibyear.add(String.format(
                                    "%s %s (%s) => %s (%s)",
                                    ArtifactIdUtils.toVersionlessId(artifact),
                                    libYear.currentVersion,
                                    libYear.currentVersionDate == null ? "?" : libYear.currentVersionDate,
                                    libYear.latestVersion,
                                    libYear.latestVersionDate == null ? "?" : libYear.latestVersionDate));
                        }
                    }
                }
            }

            output.marker(Output.Verbosity.NORMAL)
                    .emphasize("Calculated libYear for {}")
                    .say(subject);
            output.tell("");

            if (!outdatedWithLibyear.isEmpty()) {
                output.marker(Output.Verbosity.NORMAL)
                        .emphasize("  Outdated versions with known age")
                        .say();
                outdatedWithLibyear.values().stream()
                        .flatMap(Collection::stream)
                        .forEach(l -> output.marker(Output.Verbosity.NORMAL)
                                .scary("  " + l)
                                .say());
                output.tell("");
            }
            if (!outdatedWithoutLibyear.isEmpty()) {
                output.marker(Output.Verbosity.NORMAL)
                        .emphasize("  Outdated versions without known age")
                        .say();
                outdatedWithoutLibyear.forEach(l ->
                        output.marker(Output.Verbosity.NORMAL).scary("  " + l).say());
                output.tell("");
            }
            if (upToDate) {
                if (!upToDateByAge.isEmpty()) {
                    output.marker(Output.Verbosity.NORMAL)
                            .emphasize("  Up to date versions")
                            .say();
                    upToDateByAge.forEach(l -> output.marker(Output.Verbosity.NORMAL)
                            .outstanding("  " + l)
                            .say());
                    output.tell("");
                }
            }
            output.marker(Output.Verbosity.TIGHT)
                    .emphasize("Total of {} years from {} outdated dependencies for {}")
                    .say(
                            String.format("%.2f", getTotalLibyear()),
                            outdatedWithLibyear.values().stream()
                                            .mapToInt(List::size)
                                            .sum()
                                    + outdatedWithoutLibyear.size(),
                            subject);
            output.tell("");
        } finally {
            searchBackends.forEach(b -> {
                try {
                    b.close();
                } catch (Exception e) {
                    output.tell("Could not close SearchBackend", e);
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
