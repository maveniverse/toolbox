/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.gav;

import eu.maveniverse.maven.toolbox.plugin.GavSearchMojoSupport;
import eu.maveniverse.maven.toolbox.shared.Result;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.search.api.MAVEN;
import org.apache.maven.search.api.Record;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.Version;
import picocli.CommandLine;

/**
 * Lists remote repository by given "gavoid" (G or G:A or G:A:V where V may be a version constraint).
 */
@CommandLine.Command(
        name = "list",
        description = "Lists remote repository by given 'gavoid' (G or G:A or G:A:V where V may be version constraint)")
@Mojo(name = "gav-list", requiresProject = false, threadSafe = true)
public class GavListMojo extends GavSearchMojoSupport {
    /**
     * The GAV-oid to list (G or G:A or G:A:V), where V may be a version constraint.
     */
    @CommandLine.Parameters(index = "0", description = "The GAV-oid to list (G or G:A or G:A:V)")
    @Parameter(property = "gavoid", required = true)
    private String gavoid;

    /**
     * Artifact version matcher spec string to filter listed versions, if not set, all versions are listed.
     */
    @CommandLine.Option(
            names = {"--artifactVersionMatcherSpec"},
            description = "Artifact version matcher spec to filter listed versions (default: none, list all)")
    @Parameter(property = "artifactVersionMatcherSpec")
    private String artifactVersionMatcherSpec;

    /**
     * ISO-8601 timestamp (like {@code 2025-01-01T00:00:00Z}). If set, and "gavoid" is {@code G:A} and
     * artifact version matcher spec is set as well, a Resolver version filter expression is emitted that limits the
     * matched versions to those that existed at that timestamp; handy to reproduce what a range resolved to at that
     * point in time.
     */
    @CommandLine.Option(
            names = {"--timestamp"},
            description =
                    "ISO-8601 timestamp; with G:A and version matcher emits version filter expression for versions existing at it")
    @Parameter(property = "timestamp")
    private String timestamp;

    @Override
    protected Result<List<Record>> doExecute() throws Exception {
        ToolboxCommando toolboxCommando = getToolboxCommando();
        Instant at = timestamp == null ? null : Instant.parse(timestamp);
        Result<List<Record>> result = toolboxCommando.list(
                getRemoteRepository(toolboxCommando),
                gavoid,
                null,
                artifactVersionMatcherSpec == null
                        ? null
                        : toolboxCommando.parseArtifactVersionMatcherSpec(artifactVersionMatcherSpec));
        String[] elements = gavoid.split(":");
        if (at != null && artifactVersionMatcherSpec != null && result.isSuccess() && elements.length == 2) {
            emitVersionFilter(
                    elements[0] + ":" + elements[1], at, result.getData().orElseThrow());
        }
        return result;
    }

    /**
     * Emits version filter expression that makes matched versions resolve as they would at given instant: a single
     * inclusion filter whose version constraint is a union of ranges, covering versions that existed at given instant.
     * Ranges are split by versions published after given instant (like a late patch release of an older line), and
     * versions above the highest existing one are left out.
     */
    private void emitVersionFilter(String ga, Instant at, List<Record> records) throws Exception {
        GenericVersionScheme versionScheme = new GenericVersionScheme();
        TreeMap<Version, Boolean> versions = new TreeMap<>(); // version -> existed at instant
        for (Record record : records) {
            if (!record.hasField(MAVEN.VERSION)) {
                continue;
            }
            String version = record.getValue(MAVEN.VERSION);
            if (record.getLastUpdated() == null) {
                getOutput().warn("No timestamp for version {}; cannot tell if it existed at {}", version, at);
            } else {
                versions.put(
                        versionScheme.parseVersion(version),
                        !Instant.ofEpochMilli(record.getLastUpdated()).isAfter(at));
            }
        }
        ArrayList<String> ranges = new ArrayList<>();
        Version runStart = null;
        Version runEnd = null;
        for (Map.Entry<Version, Boolean> entry : versions.entrySet()) {
            if (entry.getValue()) {
                if (runStart == null) {
                    runStart = entry.getKey();
                }
                runEnd = entry.getKey();
            } else if (runStart != null) {
                ranges.add(range(runStart, runEnd));
                runStart = null;
            }
        }
        if (runStart != null) {
            ranges.add(range(runStart, runEnd));
        }
        if (ranges.isEmpty()) {
            getOutput().warn("No matched version existed at {}", at);
        } else {
            getOutput().tell("");
            getOutput().tell("Version filter expression for {} at {}:", ga, at);
            getOutput().tell("i({})@{}", String.join(",", ranges), ga);
        }
    }

    private static String range(Version start, Version end) {
        return start.equals(end) ? "[" + start + "]" : "[" + start + "," + end + "]";
    }
}
