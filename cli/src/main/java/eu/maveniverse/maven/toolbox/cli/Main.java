/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.cli;

import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import picocli.CommandLine;

/**
 * Main.
 */
@CommandLine.Command(
        name = "toolbox",
        subcommands = {
            Classpath.class,
            Copy.class,
            CopyTransitive.class,
            Deploy.class,
            DeployRecorded.class,
            Dump.class,
            Exists.class,
            Identify.class,
            Install.class,
            List.class,
            ListAvailablePlugins.class,
            ListRepositories.class,
            Record.class,
            Repl.class,
            Resolve.class,
            ResolveTransitive.class,
            Search.class,
            Tree.class,
            Verify.class
        },
        versionProvider = Main.class,
        description = "Toolbox CLI",
        mixinStandardHelpOptions = true)
public class Main extends CommandSupport {
    @Override
    protected boolean doCall(ToolboxCommando commando) throws Exception {
        commando.dump(false, output);
        return new Repl().doCall(commando);
    }

    public static void main(String... args) {
        System.exit(new CommandLine(new Main()).execute(args));
    }
}
