/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.cli;

import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import java.util.stream.Collectors;
import org.eclipse.aether.artifact.DefaultArtifact;
import picocli.CommandLine;

/**
 * Resolves a given GAV and copies resulting artifact to target.
 */
@CommandLine.Command(name = "copy", description = "Resolves Maven Artifact and copies it to target")
public final class Copy extends ResolverCommandSupport {
    @CommandLine.Parameters(index = "0", description = "The target spec", arity = "1")
    private String targetSpec;

    @CommandLine.Parameters(index = "1..*", description = "The GAVs to resolve", arity = "1")
    private java.util.List<String> gav;

    @CommandLine.Option(
            names = {"--boms"},
            defaultValue = "",
            split = ",",
            description = "Comma separated list of BOMs to apply")
    private java.util.List<String> boms;

    @Override
    protected boolean doExecute(ToolboxCommando toolboxCommando) throws Exception {
        return toolboxCommando.copy(
                gav.stream().map(DefaultArtifact::new).collect(Collectors.toList()),
                toolboxCommando.artifactSink(output, targetSpec),
                output);
    }
}
