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
 * Lists remote repository by given "gavoid" (G or G:A or G:A:V where V may be version constraint).
 */
@CommandLine.Command(name = "list", description = "Lists Maven Artifacts")
public final class List extends SearchCommandSupport {

    @CommandLine.Parameters(index = "0", description = "The GAV-oid to list (G or G:A or G:A:V)")
    private String gavoid;

    @Override
    protected Integer doCall() throws IOException {
        return ToolboxCommando.getOrCreate(getContext()).list(getRemoteRepository(), gavoid, logger) ? 0 : 1;
    }
}
