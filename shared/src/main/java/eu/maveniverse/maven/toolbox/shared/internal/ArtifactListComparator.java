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
import java.util.ArrayList;
import java.util.List;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;
import org.jline.jansi.Ansi;

/**
 * An artifact list comparator (diff).
 */
public class ArtifactListComparator {
    private final Output output;

    public ArtifactListComparator(Output output) {
        this.output = requireNonNull(output);
    }

    public void compare(String label1, List<Artifact> a1, String label2, List<Artifact> a2) {
        ArrayList<org.eclipse.aether.artifact.Artifact> a1Sa2 = new ArrayList<>(a1);
        a1Sa2.removeAll(a2);
        ArrayList<org.eclipse.aether.artifact.Artifact> a2Sa1 = new ArrayList<>(a2);
        a2Sa1.removeAll(a1);

        Marker marker = output.marker(output.getVerbosity());
        String diffSame = Ansi.ansi().fg(Ansi.Color.WHITE).a("   ").reset().toString();
        String diffAdded = Ansi.ansi().fg(Ansi.Color.GREEN).a("+++").reset().toString();
        String diffRemoved = Ansi.ansi().fg(Ansi.Color.RED).a("---").reset().toString();
        if (a1Sa2.isEmpty() && a2Sa1.isEmpty()) {
            output.tell(marker.outstanding("No differences found.").toString());
        } else {
            output.tell(marker.outstanding(label1).toString());
            a1.forEach(a -> {
                if (a2.contains(a)) {
                    output.tell(diffSame + " "
                            + marker.normal(ArtifactIdUtils.toId(a)).toString());
                } else {
                    output.tell(diffRemoved + " "
                            + marker.bloody(ArtifactIdUtils.toId(a)).toString());
                }
            });
            output.tell(marker.outstanding(label2).toString());
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
