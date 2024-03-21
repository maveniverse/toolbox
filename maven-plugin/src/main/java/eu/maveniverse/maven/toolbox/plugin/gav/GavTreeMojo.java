/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.gav;

import eu.maveniverse.maven.toolbox.shared.ResolutionScope;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.resolution.ArtifactDescriptorException;

@Mojo(name = "gav-tree", requiresProject = false, threadSafe = true)
public class GavTreeMojo extends GavMojoSupport {
    /**
     * The artifact coordinates in the format {@code <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>}
     * to display tree for.
     */
    @Parameter(property = "gav", required = true)
    private String gav;

    /**
     * The resolution scope to display, accepted values are "runtime", "compile", "test", etc.
     */
    @Parameter(property = "scope", defaultValue = "runtime", required = true)
    private String scope;

    /**
     * Set it {@code true} for verbose tree.
     */
    @Parameter(property = "verboseTree", defaultValue = "false", required = true)
    private boolean verboseTree;

    /**
     * Apply BOMs, if needed.
     */
    @Parameter(property = "boms", defaultValue = "")
    private java.util.List<String> boms;

    @Override
    protected void doExecute(ToolboxCommando toolboxCommando) throws MojoExecutionException, MojoFailureException {
        try {
            toolboxCommando.tree(ResolutionScope.parse(scope), toolboxCommando.loadGav(gav, boms), verboseTree, output);
        } catch (RuntimeException e) {
            throw new MojoExecutionException(e);
        } catch (ArtifactDescriptorException e) {
            throw new MojoFailureException(e);
        }
    }
}
