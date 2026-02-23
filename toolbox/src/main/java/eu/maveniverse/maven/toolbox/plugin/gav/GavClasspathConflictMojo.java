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
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.artifact.Artifact;
import picocli.CommandLine;

/**
 * Resolves two Maven Artifact and prints out the classpath conflicts.
 */
@CommandLine.Command(
        name = "classpath-conflict",
        description = "Resolves two Maven Artifact and prints out the classpath conflicts")
@Mojo(name = "gav-classpath-conflict", requiresProject = false, threadSafe = true)
public class GavClasspathConflictMojo extends GavMojoSupport {
    /**
     * The first artifact coordinates in the format {@code <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>}
     * to display conflicts for.
     */
    @CommandLine.Parameters(index = "0", description = "The first GAV to compare classpath")
    @Parameter(property = "gav1", required = true)
    private String gav1;

    /**
     * The second artifact coordinates in the format {@code <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>}
     * to display conflicts for.
     */
    @CommandLine.Parameters(index = "1", description = "The second GAV to compare classpath")
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

    /**
     * The artifact key factory to apply (defines sets of possibly conflicting artifacts).
     */
    @CommandLine.Option(
            names = {"--keyFactorySpec"},
            defaultValue = "versionlessId()",
            description = "The artifact key factory to apply (defines sets of possibly conflicting artifacts).")
    @Parameter(property = "keyFactorySpec", defaultValue = "versionlessId()", required = true)
    private String keyFactorySpec;

    /**
     * The artifact differentiators to apply to set of artifacts. If differentiator comes up with more than
     * one partition, you have a conflict. Spec string may contain multiple differentiators split by comma.
     */
    @CommandLine.Option(
            names = {"--artifactDifferentiatorSpec"},
            defaultValue = "majorVersion()",
            description =
                    "The artifact differentiators to apply to set of conflicting artifacts. If differentiator comes up with more than one partition, you have a conflict.")
    @Parameter(property = "artifactDifferentiatorSpec", defaultValue = "majorVersion()", required = true)
    private String artifactDifferentiatorSpec;

    @Override
    protected Result<Map<String, String>> doExecute() throws Exception {
        ToolboxCommando toolboxCommando = getToolboxCommando();
        Map<String, Function<Artifact, String>> differentiators = new HashMap<>();
        for (String differentiatorSpec : artifactDifferentiatorSpec.split(",")) {
            differentiators.put(
                    differentiatorSpec, toolboxCommando.parseArtifactDifferentiatorSpec(differentiatorSpec));
        }
        return toolboxCommando.classpathConflict(
                ResolutionScope.parse(scope),
                toolboxCommando.loadGav(gav1, slurp(boms)),
                toolboxCommando.loadGav(gav2, slurp(boms)),
                toolboxCommando.parseArtifactKeyFactorySpec(keyFactorySpec),
                differentiators);
    }
}
