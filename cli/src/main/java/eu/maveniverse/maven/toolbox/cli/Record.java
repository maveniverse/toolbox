/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.cli;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import org.eclipse.aether.resolution.DependencyResolutionException;
import picocli.CommandLine;

/**
 * Records resolved artifacts.
 */
@CommandLine.Command(name = "record", description = "Records resolved Maven Artifacts")
public final class Record extends ResolverCommandSupport {

    @Override
    protected boolean doCall(Context context) throws DependencyResolutionException {
        return ToolboxCommando.getOrCreate(context).artifactRecorder().setActive(true);
    }
}
