/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin;

import eu.maveniverse.maven.toolbox.shared.ProjectArtifacts;
import java.io.File;

/**
 * Support class for "project unaware" Mojos (not needing project).
 */
public abstract class GavMojoSupport extends MojoSupport {
    /**
     * Makes {@link ProjectArtifacts} out of supplied elements.
     */
    protected ProjectArtifacts projectArtifacts(String gav, File jar, File pom, File sources, File javadoc) {
        ProjectArtifacts.Builder builder = new ProjectArtifacts.Builder(gav);
        builder.addMain(jar.toPath());
        if (pom != null) {
            builder.addPom(pom.toPath());
        }
        if (sources != null) {
            builder.addSources(sources.toPath());
        }
        if (javadoc != null) {
            builder.addJavadoc(javadoc.toPath());
        }
        return builder.build();
    }
}
