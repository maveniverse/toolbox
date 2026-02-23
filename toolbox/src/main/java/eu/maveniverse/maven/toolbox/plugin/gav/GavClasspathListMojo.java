/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.gav;

import static eu.maveniverse.maven.toolbox.shared.input.StringSlurper.slurp;

import eu.maveniverse.maven.toolbox.plugin.GavMojoSupport;
import eu.maveniverse.maven.toolbox.shared.ResolutionScope;
import eu.maveniverse.maven.toolbox.shared.Result;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import java.util.List;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.artifact.Artifact;
import picocli.CommandLine;

/**
 * Resolves Maven Artifact and prints out the classpath as artifacts.
 */
@CommandLine.Command(
        name = "classpath-list",
        description = "Resolves Maven Artifact and prints out the classpath as artifacts")
@Mojo(name = "gav-classpath-list", requiresProject = false, threadSafe = true)
public class GavClasspathListMojo extends GavMojoSupport {
    /**
     * The artifact coordinates in the format {@code <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>}
     * to display tree for. May contain multiple comma separated coordinates, or point to a file that contains
     * coordinates, separated by newline.
     */
    @CommandLine.Parameters(index = "0", description = "The GAV to print classpath for")
    @Parameter(property = "gav", required = true)
    private String gav;

    /**
     * The resolution scope to display, accepted values are "runtime", "compile", "test", etc.
     */
    @CommandLine.Option(
            names = {"--scope"},
            defaultValue = "runtime",
            description = "Resolution scope to resolve (default 'runtime')")
    @Parameter(property = "scope", defaultValue = "runtime", required = true)
    private String scope;

    /**
     * Apply BOMs, if needed. Comma separated GAVs.
     */
    @CommandLine.Option(
            names = {"--boms"},
            defaultValue = "",
            description = "Comma separated list of BOMs to apply")
    @Parameter(property = "boms")
    private String boms;

    /**
     * Set it {@code true} for details listed.
     */
    @CommandLine.Option(
            names = {"--details"},
            defaultValue = "false",
            description = "Make it true for details listed")
    @Parameter(property = "details", defaultValue = "false", required = true)
    private boolean details;

    @Override
    protected Result<List<Artifact>> doExecute() throws Exception {
        ToolboxCommando toolboxCommando = getToolboxCommando();
        return toolboxCommando.classpathList(
                ResolutionScope.parse(scope), toolboxCommando.loadGavs(slurp(gav), slurp(boms)), details);
    }
}
