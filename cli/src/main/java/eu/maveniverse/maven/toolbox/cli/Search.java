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
 * Searches artifacts using SMO service.
 */
@CommandLine.Command(name = "search", description = "Searches Maven Artifacts")
public final class Search extends SearchCommandSupport {

    @CommandLine.Parameters(index = "0", description = "The expression to search for")
    private String expression;

    @Override
    protected Integer doCall() throws IOException {
        return ToolboxCommando.getOrCreate(getContext()).search(getRemoteRepository(), expression, logger) ? 0 : 1;
    }
}
