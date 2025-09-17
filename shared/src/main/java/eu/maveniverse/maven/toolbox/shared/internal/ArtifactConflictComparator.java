/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.toolbox.shared.output.Marker;
import eu.maveniverse.maven.toolbox.shared.output.Output;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;

/**
 * An artifact list comparator (conflict).
 */
public class ArtifactConflictComparator {
    private final Output output;
    private final Function<Artifact, String> keyFactory;
    private final Map<String, Function<Artifact, String>> differentiators;

    public ArtifactConflictComparator(
            Output output,
            Function<Artifact, String> keyFactory,
            Map<String, Function<Artifact, String>> differentiators) {
        this.output = requireNonNull(output);
        this.keyFactory = requireNonNull(keyFactory);
        this.differentiators = requireNonNull(differentiators);
    }

    public void compare(Artifact artifact1, List<Artifact> a1, Artifact artifact2, List<Artifact> a2) {
        // key -> differentiator -> variants -> member
        Map<String, Map<String, Map<String, Set<Artifact>>>> conflicts = new LinkedHashMap<>();
        a1.forEach(a -> {
            Map<String, Map<String, Set<Artifact>>> diff =
                    conflicts.computeIfAbsent(keyFactory.apply(a), k -> new LinkedHashMap<>());
            for (Map.Entry<String, Function<Artifact, String>> entry : differentiators.entrySet()) {
                diff.computeIfAbsent(entry.getKey(), k -> new LinkedHashMap<>())
                        .computeIfAbsent(entry.getValue().apply(a), k -> new LinkedHashSet<>())
                        .add(a);
            }
        });
        a2.forEach(a -> {
            Map<String, Map<String, Set<Artifact>>> diff =
                    conflicts.computeIfAbsent(keyFactory.apply(a), k -> new LinkedHashMap<>());
            for (Map.Entry<String, Function<Artifact, String>> entry : differentiators.entrySet()) {
                diff.computeIfAbsent(entry.getKey(), k -> new LinkedHashMap<>())
                        .computeIfAbsent(entry.getValue().apply(a), k -> new LinkedHashSet<>())
                        .add(a);
            }
        });

        // if key -> differentiator -> diff contains two keys => conflict
        Set<String> differentiatorsWithConflicts = new LinkedHashSet<>();
        for (Map.Entry<String, Map<String, Map<String, Set<Artifact>>>> entry : conflicts.entrySet()) {
            entry.getValue().entrySet().stream()
                    .filter(e -> e.getValue().size() > 1)
                    .forEach(e -> differentiatorsWithConflicts.add(e.getKey()));
        }

        // produce output
        Marker marker = output.marker(output.getVerbosity());
        if (differentiatorsWithConflicts.isEmpty()) {
            output.tell(
                    marker.outstanding("No conflicts exist for {} vs {}").toString(),
                    ArtifactIdUtils.toId(artifact1),
                    ArtifactIdUtils.toId(artifact2));
        } else {
            output.tell(
                    marker.scary("Conflicts exist for {} vs {}").toString(),
                    ArtifactIdUtils.toId(artifact1),
                    ArtifactIdUtils.toId(artifact2));
            boolean keyShown;
            for (Map.Entry<String, Map<String, Map<String, Set<Artifact>>>> l1 : conflicts.entrySet()) {
                keyShown = false;
                for (Map.Entry<String, Map<String, Set<Artifact>>> l2 :
                        l1.getValue().entrySet()) {
                    if (differentiatorsWithConflicts.contains(l2.getKey())
                            && l2.getValue().size() > 1) {
                        if (!keyShown) {
                            output.tell("-- {}", marker.emphasize(l1.getKey()));
                            keyShown = true;
                        }
                        output.tell("     {}", marker.normal(l2.getKey()));
                        for (Map.Entry<String, Set<Artifact>> l3 : l2.getValue().entrySet()) {
                            if (differentiatorsWithConflicts.contains(l2.getKey())) {
                                output.tell(
                                        "      {} with {}",
                                        marker.bloody(l3.getKey()).toString(),
                                        marker.scary(l3.getValue().stream()
                                                        .map(ArtifactIdUtils::toId)
                                                        .collect(Collectors.joining(", ")))
                                                .toString());
                            }
                        }
                    }
                }
            }
        }
    }
}
