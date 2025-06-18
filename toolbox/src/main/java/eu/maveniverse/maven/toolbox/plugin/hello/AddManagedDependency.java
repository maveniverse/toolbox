/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.hello;

import eu.maveniverse.maven.toolbox.shared.Result;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import java.util.Collections;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.artifact.Artifact;
import picocli.CommandLine;

/**
 * Adds managed dependency.
 */
@CommandLine.Command(name = "add-managed-dependency", description = "Adds managed dependency")
@Mojo(name = "add-managed-dependency", requiresProject = false, threadSafe = true)
public class AddManagedDependency extends HelloProjectMojoSupport {
    /**
     * The dependency GAV.
     */
    @CommandLine.Parameters(index = "0", description = "The dependency GAV", arity = "1")
    @Parameter(property = "gav", required = true)
    private String gav;

    @Override
    protected Result<Boolean> doExecute() throws Exception {
        Artifact dependency = toDependencyArtifact(gav);
        try (ToolboxCommando.EditSession editSession = getToolboxCommando().createEditSession(getRootPom())) {
            getToolboxCommando()
                    .editPom(editSession, Collections.singletonList(s -> s.updateManagedDependency(true, dependency)));
        }
        return Result.success(Boolean.TRUE);
    }
}
