/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.mp;

import eu.maveniverse.maven.toolbox.plugin.MPMojoSupport;
import eu.maveniverse.maven.toolbox.shared.Result;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Dumps the MIMA environment.
 */
@Mojo(name = "dump", threadSafe = true)
public class DumpMojo extends MPMojoSupport {
    @Override
    protected Result<String> doExecute() {
        return getToolboxCommando().dump();
    }
}
