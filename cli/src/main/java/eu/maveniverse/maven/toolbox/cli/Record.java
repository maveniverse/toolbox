/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.cli;

import eu.maveniverse.maven.mima.context.Context;
import org.eclipse.aether.resolution.DependencyResolutionException;
import picocli.CommandLine;

/**
 * Records resolved artifacts.
 */
@CommandLine.Command(name = "record", description = "Records resolved Maven Artifacts")
public final class Record extends ResolverCommandSupport {

    @CommandLine.Option(
            names = {"--stop"},
            description = "Stop recording (otherwise it starts it)")
    private boolean stop;

    @Override
    protected boolean doCall(Context context) throws DependencyResolutionException {
        if (stop) {
            return getToolboxCommando(context).recordStop(output);
        } else {
            return getToolboxCommando(context).recordStart(output);
        }
    }
}
