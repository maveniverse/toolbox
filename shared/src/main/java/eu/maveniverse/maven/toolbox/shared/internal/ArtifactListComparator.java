/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.toolbox.shared.output.Marker;
import eu.maveniverse.maven.toolbox.shared.output.Output;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;
import org.jline.jansi.Ansi;

/**
 * An artifact list comparator (diff).
 */
public class ArtifactListComparator {
    private final Output output;
    private final boolean unified;

    public ArtifactListComparator(Output output, boolean unified) {
        this.output = requireNonNull(output);
        this.unified = unified;
    }

    public void compare(Artifact a1root, List<Artifact> a1, Artifact a2root, List<Artifact> a2) {
        ArrayList<org.eclipse.aether.artifact.Artifact> a1Sa2 = new ArrayList<>(a1);
        a1Sa2.removeAll(a2);
        ArrayList<org.eclipse.aether.artifact.Artifact> a2Sa1 = new ArrayList<>(a2);
        a2Sa1.removeAll(a1);

        Marker marker = output.marker(output.getVerbosity());
        if (a1Sa2.isEmpty() && a2Sa1.isEmpty()) {
            output.tell(marker.outstanding("No differences found.").toString());
        } else if (unified) {
            // GACE -> List<Artifact> idx=0 is left, idx=1 is right
            LinkedHashMap<String, List<Artifact>> umap = new LinkedHashMap<>();
            a1.forEach(a -> umap.computeIfAbsent(ArtifactIdUtils.toVersionlessId(a), k -> new ArrayList<>())
                    .add(a));
            a2.forEach(a -> umap.computeIfAbsent(ArtifactIdUtils.toVersionlessId(a), k -> {
                        List<Artifact> list = new ArrayList<>();
                        list.add(null);
                        return list;
                    })
                    .add(a));

            String diffSame = Ansi.ansi().fg(Ansi.Color.WHITE).a("   ").reset().toString();
            String diffModified =
                    Ansi.ansi().fg(Ansi.Color.YELLOW).a("***").reset().toString();
            String diffAdded = Ansi.ansi().fg(Ansi.Color.GREEN).a("+++").reset().toString();
            String diffRemoved = Ansi.ansi().fg(Ansi.Color.RED).a("---").reset().toString();

            for (Map.Entry<String, List<Artifact>> entry : umap.entrySet()) {
                Artifact left = entry.getValue().get(0);
                Artifact right = entry.getValue().size() == 2 ? entry.getValue().get(1) : null;
                if (Objects.equals(left, right)) {
                    output.tell(diffSame + " "
                            + marker.normal(ArtifactIdUtils.toId(left)).toString());
                } else if (left == null) {
                    output.tell(diffAdded + " "
                            + marker.outstanding(ArtifactIdUtils.toId(right)).toString());
                } else if (right == null) {
                    output.tell(diffRemoved + " "
                            + marker.bloody(ArtifactIdUtils.toId(left)).toString());
                } else {
                    output.tell(diffModified + " "
                            + marker.normal(ArtifactIdUtils.toVersionlessId(left))
                                    .toString() + " "
                            + marker.bloody(left.getVersion()).toString() + " -> "
                            + marker.bloody(right.getVersion()).toString());
                }
            }
        } else {
            String diffSame = Ansi.ansi().fg(Ansi.Color.WHITE).a("   ").reset().toString();
            String diffAdded = Ansi.ansi().fg(Ansi.Color.GREEN).a("+++").reset().toString();
            String diffRemoved = Ansi.ansi().fg(Ansi.Color.RED).a("---").reset().toString();
            output.tell(marker.outstanding(String.format("Classpath of %s (in order)", ArtifactIdUtils.toId(a1root)))
                    .toString());
            a1.forEach(a -> {
                if (a2.contains(a)) {
                    output.tell(diffSame + " "
                            + marker.normal(ArtifactIdUtils.toId(a)).toString());
                } else {
                    output.tell(diffRemoved + " "
                            + marker.bloody(ArtifactIdUtils.toId(a)).toString());
                }
            });
            output.tell(marker.outstanding(String.format("Classpath of %s (in order)", ArtifactIdUtils.toId(a2root)))
                    .toString());
            a2.forEach(a -> {
                if (a1.contains(a)) {
                    output.tell(diffSame + " "
                            + marker.normal(ArtifactIdUtils.toId(a)).toString());
                } else {
                    output.tell(diffAdded + " "
                            + marker.outstanding(ArtifactIdUtils.toId(a)).toString());
                }
            });
        }
    }
}
