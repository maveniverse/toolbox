/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.mp;

import eu.maveniverse.maven.toolbox.plugin.MPMojoSupport;
import eu.maveniverse.maven.toolbox.shared.ResolutionScope;
import eu.maveniverse.maven.toolbox.shared.Result;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import java.util.List;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.repository.RemoteRepository;
import org.slf4j.Logger;

/**
 * Resolves transitively current project and outputs used repositories.
 */
@Mojo(name = "list-repositories", threadSafe = true)
public final class ListRepositoriesMojo extends MPMojoSupport {
    /**
     * The resolution scope to display, accepted values are "runtime", "compile", "test", etc.
     */
    @Parameter(property = "scope", defaultValue = "runtime", required = true)
    private String scope;

    @Override
    protected Result<List<RemoteRepository>> doExecute(Logger output, ToolboxCommando toolboxCommando)
            throws Exception {
        return toolboxCommando.listRepositories(
                ResolutionScope.parse(scope), "project", projectAsResolutionRoot(), output);
    }
}
