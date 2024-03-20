/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.cli;

import eu.maveniverse.maven.mima.context.Context;
import picocli.CommandLine;

/**
 * List plugins.
 */
@CommandLine.Command(name = "listPlugins", description = "List plugins")
public final class ListPlugins extends ResolverCommandSupport {

    @CommandLine.Parameters(index = "0", description = "The G to list")
    private String g;

    @Override
    protected Integer doCall(Context context) throws Exception {
        //        Toolbox toolbox = Toolbox.getOrCreate(context);
        //
        //        toolbox.listAvailablePlugins(Collections.singletonList(g), context.remoteRepositories()).stream()
        //                .sorted(Comparator.comparing(ArtifactIdUtils::toId))
        //                .forEach(p -> info(p.toString()));
        return 0;
    }
}
