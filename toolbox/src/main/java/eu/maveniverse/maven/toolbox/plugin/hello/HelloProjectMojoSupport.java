/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.hello;

import eu.maveniverse.maven.toolbox.shared.internal.jdom.JDomDocumentIO;
import eu.maveniverse.maven.toolbox.shared.internal.jdom.JDomUtils;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.jdom2.Element;

/**
 * Support class for "hello" Mojos (with existing project).
 */
public abstract class HelloProjectMojoSupport extends HelloMojoSupport {
    protected final Artifact currentProjectArtifact;

    public HelloProjectMojoSupport() {
        if (!Files.exists(rootPom)) {
            throw new IllegalStateException("This directory does not contain pom.xml");
        }
        try (JDomDocumentIO documentIO = new JDomDocumentIO(rootPom)) {
            Element project = documentIO.getDocument().getRootElement();
            Element parent = project.getChild("parent", project.getNamespace());

            String groupId = JDomUtils.getChildElementTextTrim("groupId", project);
            if (groupId == null && parent != null) {
                groupId = JDomUtils.getChildElementTextTrim("groupId", parent);
            }
            String artifactId = JDomUtils.getChildElementTextTrim("artifactId", project);
            if (artifactId == null && parent != null) {
                artifactId = JDomUtils.getChildElementTextTrim("artifactId", parent);
            }
            String version = JDomUtils.getChildElementTextTrim("version", project);
            if (version == null && parent != null) {
                version = JDomUtils.getChildElementTextTrim("version", parent);
            }
            this.currentProjectArtifact =
                    new DefaultArtifact(groupId, artifactId, "pom", version).setFile(rootPom.toFile());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Accepts {@code A}, {@code G:A} or {@code G:A:V}. Missing pieces are combined with current project.
     */
    protected Artifact toSubProjectArtifact(String gav) {
        Artifact result;
        try {
            result = new DefaultArtifact(gav);
        } catch (IllegalArgumentException ex) {
            try {
                result = new DefaultArtifact(gav + ":" + currentProjectArtifact.getVersion());
            } catch (IllegalArgumentException ex2) {
                try {
                    if (gav.startsWith(".") && gav.contains(":")) {
                        result = new DefaultArtifact(
                                currentProjectArtifact.getGroupId() + gav + ":" + currentProjectArtifact.getVersion());
                    } else {
                        result = new DefaultArtifact(currentProjectArtifact.getGroupId() + ":" + gav + ":"
                                + currentProjectArtifact.getVersion());
                    }
                } catch (IllegalArgumentException ex3) {
                    throw new IllegalArgumentException("Invalid gav: " + gav);
                }
            }
        }
        return result.setFile(rootPom.resolve(result.getArtifactId()).toFile());
    }
}
