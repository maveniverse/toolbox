/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.mp;

import eu.maveniverse.maven.toolbox.plugin.MPPluginMojoSupport;
import eu.maveniverse.maven.toolbox.shared.ArtifactMatcher;
import eu.maveniverse.maven.toolbox.shared.ResolutionRoot;
import eu.maveniverse.maven.toolbox.shared.ResolutionScope;
import eu.maveniverse.maven.toolbox.shared.Result;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import eu.maveniverse.maven.toolbox.shared.output.Output;
import java.util.List;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.Exclusion;

/**
 * Checks present exclusions.
 */
@Mojo(name = "check-exclusions", threadSafe = true)
public class CheckExclusionsMojo extends MPPluginMojoSupport {
    /**
     * Set it {@code true} for verbose tree.
     */
    @Parameter(property = "verboseTree", defaultValue = "false", required = true)
    private boolean verboseTree;

    @Override
    protected Result<Boolean> doExecute() throws Exception {
        ToolboxCommando toolboxCommando = getToolboxCommando();
        ResolutionRoot project = projectAsResolutionRoot();
        for (Dependency dependency : project.getDependencies()) {
            for (Exclusion exclusion : dependency.getExclusions()) {
                ResolutionRoot dependencyRoot;
                if (isReactorDependency(dependency)) {
                    dependencyRoot =
                            ResolutionRoot.ofNotLoaded(dependency.getArtifact()).build();
                } else {
                    dependencyRoot =
                            ResolutionRoot.ofLoaded(dependency.getArtifact()).build();
                }
                getOutput()
                        .marker(Output.Verbosity.NORMAL)
                        .normal("Checking dependency ")
                        .emphasize(dependency.toString())
                        .normal(" exclusion ")
                        .emphasize(exclusion.toString())
                        .normal(" paths")
                        .say();
                Result<List<List<Artifact>>> paths = toolboxCommando.treeFind(
                        ResolutionScope.RUNTIME,
                        dependencyRoot,
                        verboseTree,
                        artifactMatcher(dependency.getArtifact(), exclusion));
                if (paths.getData().isPresent() && paths.getData().orElseThrow().isEmpty()) {
                    getOutput()
                            .marker(Output.Verbosity.NORMAL)
                            .scary("Is unused")
                            .say();
                } else {
                    getOutput()
                            .marker(Output.Verbosity.NORMAL)
                            .outstanding("Is used")
                            .say();
                }
                getOutput().doTell("");
            }
        }
        return Result.success(true);
    }

    protected ArtifactMatcher artifactMatcher(Artifact root, Exclusion exclusion) {
        // not(root) && matches
        return ArtifactMatcher.and(
                ArtifactMatcher.not(ArtifactMatcher.artifact(root.toString())),
                ArtifactMatcher.artifact(asteriskOrString(exclusion.getGroupId()) + ":"
                        + asteriskOrString(exclusion.getArtifactId()) + ":"
                        + asteriskOrString(exclusion.getClassifier())
                        + ":" + asteriskOrString(exclusion.getExtension()) + ":*"));
    }

    protected String asteriskOrString(String str) {
        if (str == null || str.trim().isEmpty()) {
            return "*";
        }
        return str;
    }
}
