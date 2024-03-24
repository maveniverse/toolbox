/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin;

import eu.maveniverse.maven.toolbox.plugin.gav.*;
import eu.maveniverse.maven.toolbox.shared.Output;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import picocli.CommandLine;

/**
 * Main CLI class.
 */
@CommandLine.Command(
        name = "toolbox",
        subcommands = {
            GavClasspathMojo.class,
            GavCopyMojo.class,
            GavCopyTransitiveMojo.class,
            GavDeployMojo.class,
            GavDeployRecordedMojo.class,
            GavDumpMojo.class,
            GavExistsMojo.class,
            GavIdentifyMojo.class,
            GavInstallMojo.class,
            GavListMojo.class,
            GavListAvailablePluginsMojo.class,
            GavListRepositoriesMojo.class,
            GavRecordMojo.class,
            GavReplMojo.class,
            GavResolveMojo.class,
            GavResolveTransitiveMojo.class,
            GavSearchMojo.class,
            GavTreeMojo.class,
            GavVerifyMojo.class
        },
        versionProvider = CLI.class,
        description = "Toolbox CLI",
        mixinStandardHelpOptions = true)
public class CLI extends MojoSupport {
    @Override
    protected boolean doExecute(Output output, ToolboxCommando commando) throws Exception {
        return new GavReplMojo().doExecute(output, commando);
    }

    public static void main(String... args) {
        System.exit(new CommandLine(new CLI()).execute(args));
    }
}
