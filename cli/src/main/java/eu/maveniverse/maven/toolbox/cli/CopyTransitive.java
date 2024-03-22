/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.cli;

import eu.maveniverse.maven.toolbox.shared.DirectorySink;
import eu.maveniverse.maven.toolbox.shared.ResolutionScope;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import java.nio.file.Path;
import picocli.CommandLine;

/**
 * Resolves transitively a given GAV and copies resulting artifacts to target.
 */
@CommandLine.Command(
        name = "copyTransitive",
        description = "Resolves Maven Artifact transitively and copies all of them to target")
public final class CopyTransitive extends ResolverCommandSupport {
    @CommandLine.Parameters(index = "0", description = "The target", arity = "1")
    private Path target;

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
    protected boolean doCall(ToolboxCommando toolboxCommando) throws Exception {
        Path targetPath = target.toAbsolutePath();
        return toolboxCommando.copyTransitive(
                ResolutionScope.parse(resolutionScope),
                toolboxCommando.loadGavs(gav, boms),
                DirectorySink.flat(output, targetPath),
                output);
    }
}
