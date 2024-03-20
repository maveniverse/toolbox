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
import java.io.IOException;
import picocli.CommandLine;

/**
 * Identify artifact, either by provided SHA-1 or calculated SHA-1 of a file pointed at.
 */
@CommandLine.Command(name = "identify", description = "Identifies Maven Artifacts")
public final class Identify extends SearchCommandSupport {

    @CommandLine.Parameters(index = "0", description = "File or sha1 checksum to identify artifact with")
    private String target;

    @Override
    protected boolean doCall(Context context) throws IOException {
        return ToolboxCommando.getOrCreate(context).identify(getRemoteRepository(), target, output);
    }
}
