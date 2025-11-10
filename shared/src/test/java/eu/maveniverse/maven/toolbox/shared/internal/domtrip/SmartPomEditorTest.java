/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal.domtrip;

import static eu.maveniverse.maven.toolbox.shared.internal.domtrip.DOMTripUtils.fromPom;

import eu.maveniverse.domtrip.Document;
import java.nio.file.Path;
import org.eclipse.aether.artifact.Artifact;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SmartPomEditorTest {
    @Test
    void smoke() {
        Path pom = Path.of("src/test/poms/lock-plugin-versions/pom.xml");
        Artifact artifact = fromPom(pom);
        Assertions.assertNotNull(artifact);
        Assertions.assertEquals("org.maveniverse.maven.toolbox.it", artifact.getGroupId());
        Assertions.assertEquals("lock-plugin-versions", artifact.getArtifactId());
        Assertions.assertEquals("1.0.0", artifact.getVersion());
        Assertions.assertEquals("pom", artifact.getExtension());
        Assertions.assertEquals("", artifact.getClassifier());
        Assertions.assertEquals(pom.toFile(), artifact.getFile());

        SmartPomEditor subject = new SmartPomEditor(Document.of(pom));
        subject.updateManagedPlugin(true, eu.maveniverse.domtrip.maven.Artifact.of("org.example", "plugin", "1.0"));
        System.out.println(subject.toXml());
    }
}
