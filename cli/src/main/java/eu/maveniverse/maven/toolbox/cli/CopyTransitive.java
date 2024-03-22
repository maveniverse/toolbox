/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.cli;

import eu.maveniverse.maven.toolbox.shared.ResolutionScope;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import picocli.CommandLine;

/**
 * Resolves transitively a given GAV and copies resulting artifacts to target.
 */
@CommandLine.Command(
        name = "copyTransitive",
        description = "Resolves Maven Artifact transitively and copies all of them to target")
public final class CopyTransitive extends ResolverCommandSupport {
    @CommandLine.Parameters(index = "0", description = "The target spec", arity = "1")
    private String targetSpec;

    @CommandLine.Parameters(index = "1..*", description = "The GAVs to resolve", arity = "1")
    private java.util.List<String> gav;

    @CommandLine.Option(
            names = {"--resolutionScope"},
            defaultValue = "runtime",
            description = "Resolution scope to resolve (default main-runtime)")
    private String resolutionScope;

    @CommandLine.Option(
            names = {"--boms"},
            defaultValue = "",
            split = ",",
            description = "Comma separated list of BOMs to apply")
    private java.util.List<String> boms;

    @Override
    protected boolean doExecute(ToolboxCommando toolboxCommando) throws Exception {
        return toolboxCommando.copyTransitive(
                ResolutionScope.parse(resolutionScope),
                toolboxCommando.loadGavs(gav, boms),
                toolboxCommando.artifactSink(output, targetSpec),
                output);
    }
}
