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
 * Main.
 */
@CommandLine.Command(
        name = "mima",
        subcommands = {
            Classpath.class,
            Deploy.class,
            DeployRecorded.class,
            Dump.class,
            Exists.class,
            Identify.class,
            Install.class,
            List.class,
            ListAvailablePlugins.class,
            Search.class,
            Record.class,
            Repl.class,
            Resolve.class,
            Tree.class,
            Verify.class
        },
        versionProvider = Main.class,
        description = "MIMA CLI",
        mixinStandardHelpOptions = true)
public class Main extends CommandSupport implements CommandLine.IVersionProvider {
    @Override
    public String[] getVersion() {
        return new String[] {"MIMA " + getRuntime().version()};
    }

    @Override
    protected Integer doCall(Context context) {
        mayDumpEnv(getRuntime(), context, false);
        return new Repl().doCall(context);
    }

    public static void main(String... args) {
        System.exit(new CommandLine(new Main()).execute(args));
    }
}
