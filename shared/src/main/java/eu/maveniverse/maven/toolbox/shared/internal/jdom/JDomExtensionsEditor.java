/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal.jdom;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.jdom2.Element;
import org.jdom2.Text;

/**
 * Methods editing JDom Extensions representation.
 */
public final class JDomExtensionsEditor {
    private static final JDomCfg jDomCfg = new JDomExtensionsCfg();

    /**
     * Helper for GA equality. To be used with extensions.
     */
    public static boolean equalsGA(Artifact artifact, Element element) {
        String groupId = element.getChildText("groupId", element.getNamespace());
        String artifactId = element.getChildText("artifactId", element.getNamespace());
        return Objects.equals(artifact.getGroupId(), groupId) && Objects.equals(artifact.getArtifactId(), artifactId);
    }

    public static List<Artifact> listExtensions(Element extensions) {
        ArrayList<Artifact> result = new ArrayList<>();
        if (extensions != null) {
            for (Element extension : extensions.getChildren("extension", extensions.getNamespace())) {
                result.add(new DefaultArtifact(
                        extension.getChildText("groupId", extension.getNamespace()),
                        extension.getChildText("artifactId", extension.getNamespace()),
                        "jar",
                        extension.getChildText("version", extension.getNamespace())));
            }
        }
        return result;
    }

    public static void updateExtension(Element extensions, Artifact a, boolean upsert) {
        if (extensions != null) {
            Element toUpdate = null;
            for (Element extension : extensions.getChildren("extension", extensions.getNamespace())) {
                if (equalsGA(a, extension)) {
                    toUpdate = extension;
                    break;
                }
            }
            if (upsert && toUpdate == null) {
                toUpdate = new Element("extension", extensions.getNamespace());
                toUpdate.addContent(new Text("\n  " + JDomUtils.detectIndentation(extensions)));
                JDomUtils.addElement(jDomCfg, toUpdate, extensions);
                JDomUtils.addElement(
                        jDomCfg, new Element("groupId", extensions.getNamespace()).setText(a.getGroupId()), toUpdate);
                JDomUtils.addElement(
                        jDomCfg,
                        new Element("artifactId", extensions.getNamespace()).setText(a.getArtifactId()),
                        toUpdate);
                JDomUtils.addElement(
                        jDomCfg, new Element("version", extensions.getNamespace()).setText(a.getVersion()), toUpdate);
                return;
            }
            if (toUpdate != null) {
                toUpdate.getChild("version", toUpdate.getNamespace()).setText(a.getVersion());
            }
        }
    }

    public static void deleteExtension(Element extensions, Artifact a) {
        if (extensions != null) {
            for (Element extension : extensions.getChildren("extension", extensions.getNamespace())) {
                if (equalsGA(a, extension)) {
                    JDomUtils.removeChildAndItsCommentFromContent(extensions, extension);
                }
            }
        }
    }
}
