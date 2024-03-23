/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.gav;

import eu.maveniverse.maven.toolbox.plugin.GavSearchMojoSupport;
import eu.maveniverse.maven.toolbox.shared.Output;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import java.io.IOException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Checks given GAV for existence in a remote repository.
 */
@Mojo(name = "gav-exists", requiresProject = false, threadSafe = true)
public class GavExistsMojo extends GavSearchMojoSupport {
    /**
     * The artifact coordinates in the format
     * {@code <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>} to check existence.
     */
    @Parameter(property = "gav", required = true)
    private String gav;

    @Parameter(property = "pom", defaultValue = "false")
    private boolean pom;

    @Parameter(property = "sources", defaultValue = "false")
    private boolean sources;

    @Parameter(property = "javadoc", defaultValue = "false")
    private boolean javadoc;

    @Parameter(property = "signature", defaultValue = "false")
    private boolean signature;

    @Parameter(property = "allRequired", defaultValue = "false")
    private boolean allRequired;

    @Override
    protected boolean doExecute(Output output, ToolboxCommando toolboxCommando) throws IOException {
        return toolboxCommando.exists(
                getRemoteRepository(toolboxCommando), gav, pom, sources, javadoc, signature, allRequired, output);
    }
}
