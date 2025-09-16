/*
 * Copyright (c) 2023-2024 Maveniverse Org.
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
import java.util.Map;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import picocli.CommandLine;

/**
 * Resolves two Maven Artifact and prints out the classpath differences.
 */
@CommandLine.Command(name = "classpath-diff", description = "Resolves Maven Artifact and prints out the classpath")
@Mojo(name = "gav-classpath-diff", requiresProject = false, threadSafe = true)
public class GavClasspathDiffMojo extends GavMojoSupport {
    /**
     * The first artifact coordinates in the format {@code <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>}
     * to display tree for.
     */
    @CommandLine.Parameters(index = "0", description = "The GAV to print classpath for")
    @Parameter(property = "gav1", required = true)
    private String gav1;

    /**
     * The second artifact coordinates in the format {@code <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>}
     * to display tree for.
     */
    @CommandLine.Parameters(index = "0", description = "The GAV to print classpath for")
    @Parameter(property = "gav2", required = true)
    private String gav2;

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

    @Override
    protected Result<Map<String, String>> doExecute() throws Exception {
        ToolboxCommando toolboxCommando = getToolboxCommando();
        return toolboxCommando.classpathDiff(
                ResolutionScope.parse(scope),
                toolboxCommando.loadGav(gav1, slurp(boms)),
                toolboxCommando.loadGav(gav2, slurp(boms)));
    }
}
