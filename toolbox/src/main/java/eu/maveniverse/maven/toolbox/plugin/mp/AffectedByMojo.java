/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.mp;

import eu.maveniverse.maven.toolbox.plugin.MPMojoSupport;
import eu.maveniverse.maven.toolbox.shared.ArtifactMatcher;
import eu.maveniverse.maven.toolbox.shared.ResolutionRoot;
import eu.maveniverse.maven.toolbox.shared.ResolutionScope;
import eu.maveniverse.maven.toolbox.shared.Result;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

/**
 * Lists reactor module artifactIds whose dependency tree contains an artifact matching the given spec.
 */
@Mojo(name = "affected-by", aggregator = true, threadSafe = true)
public class AffectedByMojo extends MPMojoSupport {
    /**
     * The resolution scope to use, accepted values are "runtime", "compile", "test", etc.
     */
    @Parameter(property = "scope", defaultValue = "runtime", required = true)
    private String scope;

    /**
     * The artifact matcher spec.
     */
    @Parameter(property = "artifactMatcherSpec", required = true)
    private String artifactMatcherSpec;

    /**
     * Optional output file. If set, the list of affected artifactIds is written to this file
     * instead of stdout.
     */
    @Parameter(property = "output")
    private File output;

    @Override
    protected Result<List<String>> doExecute() throws Exception {
        ToolboxCommando toolboxCommando = getToolboxCommando();
        ArtifactMatcher artifactMatcher = toolboxCommando.parseArtifactMatcherSpec(artifactMatcherSpec);
        ResolutionScope resolutionScope = ResolutionScope.parse(scope);

        List<String> affectedArtifactIds = new ArrayList<>();
        for (MavenProject project : mavenSession.getProjects()) {
            ResolutionRoot root = projectAsResolutionRoot(project);
            Result<List<List<Artifact>>> treeFindResult =
                    toolboxCommando.treeFind(resolutionScope, root, false, artifactMatcher);
            if (treeFindResult.isSuccess()
                    && treeFindResult.getData().isPresent()
                    && !treeFindResult.getData().orElseThrow().isEmpty()) {
                affectedArtifactIds.add(project.getArtifactId());
            }
        }

        if (output != null) {
            Files.createDirectories(output.toPath().getParent());
            try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(output.toPath(), StandardCharsets.UTF_8))) {
                for (String artifactId : affectedArtifactIds) {
                    pw.println(artifactId);
                }
            }
        } else {
            for (String artifactId : affectedArtifactIds) {
                getOutput().doTell("{}", artifactId);
            }
        }

        return Result.success(affectedArtifactIds);
    }

    private ResolutionRoot projectAsResolutionRoot(MavenProject project) {
        ResolutionRoot.Builder builder = ResolutionRoot.ofNotLoaded(new DefaultArtifact(
                project.getGroupId(),
                project.getArtifactId(),
                artifactHandlerManager
                        .getArtifactHandler(project.getPackaging())
                        .getExtension(),
                project.getVersion()));
        builder.withDependencies(toDependencies(project.getDependencies()));
        if (project.getDependencyManagement() != null) {
            builder.withManagedDependencies(
                    toDependencies(project.getDependencyManagement().getDependencies()));
        }
        return builder.build();
    }
}
