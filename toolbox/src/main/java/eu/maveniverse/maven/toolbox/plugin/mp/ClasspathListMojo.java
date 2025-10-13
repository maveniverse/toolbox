/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.mp;

import eu.maveniverse.maven.toolbox.plugin.MPMojoSupport;
import eu.maveniverse.maven.toolbox.shared.ResolutionScope;
import eu.maveniverse.maven.toolbox.shared.Result;
import java.util.Collections;
import java.util.List;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.artifact.Artifact;

/**
 * Prints the classpath of current project as list of artifacts.
 */
@Mojo(name = "classpath-list", threadSafe = true)
public class ClasspathListMojo extends MPMojoSupport {
    /**
     * The resolution scope to display, accepted values are "runtime", "compile", "test", etc.
     */
    @Parameter(property = "scope", defaultValue = "runtime", required = true)
    private String scope;

    /**
     * Set it {@code true} for details listed.
     */
    @Parameter(property = "details", defaultValue = "false", required = true)
    private boolean details;

    @Override
    protected Result<List<Artifact>> doExecute() throws Exception {
        return getToolboxCommando()
                .classpathList(
                        ResolutionScope.parse(scope), Collections.singletonList(projectAsResolutionRoot()), details);
    }
}
