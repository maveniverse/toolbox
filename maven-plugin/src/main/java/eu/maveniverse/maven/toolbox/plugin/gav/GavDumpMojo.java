/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.gav;

import eu.maveniverse.maven.toolbox.plugin.GavMojoSupport;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "gav-dump", requiresProject = false, threadSafe = true)
public class GavDumpMojo extends GavMojoSupport {
    @Override
    protected void doExecute(ToolboxCommando toolboxCommando) {
        toolboxCommando.dump(logger.isDebugEnabled(), output);
    }
}
