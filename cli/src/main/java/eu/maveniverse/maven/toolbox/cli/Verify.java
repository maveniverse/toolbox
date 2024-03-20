/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.cli;

import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import java.io.IOException;
import picocli.CommandLine;

/**
 * Verifies artifact against known SHA-1.
 */
@CommandLine.Command(name = "verify", description = "Verifies Maven Artifact")
public final class Verify extends SearchCommandSupport {

    @CommandLine.Parameters(index = "0", description = "The GAV to check")
    private String gav;

    @CommandLine.Parameters(index = "1", description = "The known SHA-1 of GAV")
    private String sha1;

    @Override
    protected Integer doCall() throws IOException {
        return ToolboxCommando.getOrCreate(getContext()).verify(getRemoteRepository(), gav, sha1, logger) ? 0 : 1;
    }
}
