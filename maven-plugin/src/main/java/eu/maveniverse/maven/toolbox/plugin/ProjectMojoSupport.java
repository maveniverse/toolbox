/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.Runtime;
import eu.maveniverse.maven.mima.context.Runtimes;
import eu.maveniverse.maven.toolbox.shared.Output;
import eu.maveniverse.maven.toolbox.shared.ResolutionRoot;
import eu.maveniverse.maven.toolbox.shared.Slf4jOutput;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import java.util.stream.Collectors;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Support class for "project aware" Mojos.
 */
public abstract class ProjectMojoSupport extends AbstractMojo {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final Output output = new Slf4jOutput(logger);

    @Parameter(property = "verbose", defaultValue = "false", required = true)
    protected boolean verbose;

    @Component
    private MavenProject mavenProject;

    @Component
    private MavenSession mavenSession;

    @Component
    private ArtifactHandlerManager artifactHandlerManager;

    protected ResolutionRoot projectAsResolutionRoot() {
        ArtifactTypeRegistry artifactTypeRegistry =
                mavenSession.getRepositorySession().getArtifactTypeRegistry();
        return ResolutionRoot.ofNotLoaded(new DefaultArtifact(
                        mavenProject.getGroupId(),
                        mavenProject.getArtifactId(),
                        artifactHandlerManager
                                .getArtifactHandler(mavenProject.getPackaging())
                                .getExtension(),
                        mavenProject.getVersion()))
                .withDependencies(mavenProject.getDependencies().stream()
                        .map(d -> RepositoryUtils.toDependency(d, artifactTypeRegistry))
                        .collect(Collectors.toList()))
                .withManagedDependencies(mavenProject.getDependencyManagement().getDependencies().stream()
                        .map(d -> RepositoryUtils.toDependency(d, artifactTypeRegistry))
                        .collect(Collectors.toList()))
                .build();
    }

    @Override
    public final void execute() throws MojoExecutionException, MojoFailureException {
        Runtime runtime = Runtimes.INSTANCE.getRuntime();
        try (Context context = runtime.create(ContextOverrides.create().build())) {
            doExecute(ToolboxCommando.create(runtime, context));
        }
    }

    protected abstract void doExecute(ToolboxCommando toolboxCommando)
            throws MojoExecutionException, MojoFailureException;
}
