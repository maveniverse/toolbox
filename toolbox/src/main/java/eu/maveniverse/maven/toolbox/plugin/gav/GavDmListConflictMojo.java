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
import eu.maveniverse.maven.toolbox.shared.Result;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import picocli.CommandLine;

/**
 * Displays dependency management list conflicts of Maven Artifact.
 */
@CommandLine.Command(name = "dm-list-conflict", description = "Displays dependency management list of Maven Artifact")
@Mojo(name = "gav-dm-list-conflict", requiresProject = false, threadSafe = true)
public class GavDmListConflictMojo extends GavMojoSupport {
    /**
     * The GAV to show tree for.
     */
    @CommandLine.Parameters(index = "0", description = "The GAV to show dependency management list for", arity = "1")
    @Parameter(property = "gav1", required = true)
    private String gav1;

    /**
     * The GAV to show tree for.
     */
    @CommandLine.Parameters(index = "0", description = "The GAV to show dependency management list for", arity = "1")
    @Parameter(property = "gav2", required = true)
    private String gav2;

    /**
     * Comma separated list of BOMs to apply.
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
    protected Result<Map<List<Dependency>, List<Dependency>>> doExecute() throws Exception {
        ToolboxCommando toolboxCommando = getToolboxCommando();
        Map<String, Function<Artifact, String>> differentiators = new HashMap<>();
        for (String differentiatorSpec : artifactDifferentiatorSpec.split(",")) {
            differentiators.put(
                    differentiatorSpec, toolboxCommando.parseArtifactDifferentiatorSpec(differentiatorSpec));
        }
        return toolboxCommando.dmListConflict(
                toolboxCommando.loadGav(gav1, slurp(boms)),
                toolboxCommando.loadGav(gav2, slurp(boms)),
                toolboxCommando.parseArtifactKeyFactorySpec(keyFactorySpec),
                differentiators);
    }
}
