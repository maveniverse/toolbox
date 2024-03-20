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
import java.util.Collections;
import picocli.CommandLine;

/**
 * List plugins.
 */
@CommandLine.Command(name = "listAvailablePlugins", description = "List available plugins")
public final class ListAvailablePlugins extends ResolverCommandSupport {

    @CommandLine.Parameters(index = "0", description = "The G to list")
    private String g;

    @Override
    protected boolean doCall(Context context) throws Exception {
        return ToolboxCommando.getOrCreate(context).listAvailablePlugins(Collections.singletonList(g), output);
    }
}
