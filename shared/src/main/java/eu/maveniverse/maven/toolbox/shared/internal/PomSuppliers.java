/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static java.util.Objects.requireNonNull;

import org.maveniverse.domtrip.maven.MavenPomElements;
import org.maveniverse.domtrip.maven.PomEditor;

/**
 * Some simple suppliers of POM.
 */
public final class PomSuppliers {
    public static String empty400(String groupId, String artifactId, String version) {
        requireNonNull(groupId, "groupId");
        requireNonNull(artifactId, "artifactId");
        requireNonNull(version, "version");
        PomEditor pomEditor = new PomEditor();
        pomEditor.createMavenDocument("project");
        pomEditor.insertMavenElement(
                pomEditor.root(),
                MavenPomElements.Elements.MODEL_VERSION,
                MavenPomElements.ModelVersions.MODEL_VERSION_4_0_0);
        pomEditor.insertMavenElement(pomEditor.root(), MavenPomElements.Elements.GROUP_ID, groupId);
        pomEditor.insertMavenElement(pomEditor.root(), MavenPomElements.Elements.ARTIFACT_ID, artifactId);
        pomEditor.insertMavenElement(pomEditor.root(), MavenPomElements.Elements.VERSION, version);
        return pomEditor.toXml();
    }
}
