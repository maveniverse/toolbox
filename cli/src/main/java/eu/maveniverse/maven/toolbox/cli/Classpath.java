/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.cli;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.toolbox.shared.ResolutionScope;
import eu.maveniverse.maven.toolbox.shared.Toolbox;
import eu.maveniverse.maven.toolbox.shared.internal.ToolboxImpl;
import java.io.File;
import java.util.stream.Collectors;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;
import picocli.CommandLine;

/**
 * Resolves transitively a given GAV and outputs classpath path.
 */
@CommandLine.Command(name = "classpath", description = "Resolves Maven Artifact and prints out the classpath")
public final class Classpath extends ResolverCommandSupport {
    @CommandLine.Parameters(index = "0", description = "The GAV to print classpath for")
    private String gav;

    @CommandLine.Option(
            names = {"--resolutionScope"},
            defaultValue = "main-runtime",
            description = "Resolution scope to resolve (default main-runtime)")
    private String resolutionScope;

    @CommandLine.Option(
            names = {"--mavenLevel"},
            defaultValue = "Maven3",
            description = "The Maven level to use (default Maven3)")
    private String mavenLevel;

    @CommandLine.Option(
            names = {"--boms"},
            defaultValue = "",
            split = ",",
            description = "Comma separated list of BOMs to apply")
    private String[] boms;

    @Override
    protected Integer doCall(Context context) throws Exception {
        java.util.List<Dependency> managedDependencies = importBoms(context, boms);
        Artifact gav = parseGav(this.gav, managedDependencies);

        ResolutionScope resolutionScope = toResolutionScope(this.resolutionScope);
        Toolbox toolbox = new ToolboxImpl(context);
        DependencyResult dependencyResult = toolbox.resolve(
                resolutionScope, new Dependency(gav, ""), null, managedDependencies, context.remoteRepositories());

        PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
        dependencyResult.getRoot().accept(nlg);
        // TODO: Do not use PreorderNodeListGenerator#getClassPath() until MRESOLVER-483 is fixed/released
        info("{}", nlg.getFiles().stream().map(File::getAbsolutePath).collect(Collectors.joining(File.pathSeparator)));
        return 0;
    }
}
