/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.mvnsh;

import eu.maveniverse.maven.toolbox.plugin.CLI;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.cling.invoker.LookupContext;
import org.apache.maven.cling.invoker.mvnsh.ShellCommandRegistryFactory;
import org.jline.console.CommandRegistry;
import picocli.CommandLine;
import picocli.shell.jline3.PicocliCommands;

/**
 * Main C.
 */
@Named("toolbox")
@Singleton
public class ToolboxShellCommandRegistryFactory implements ShellCommandRegistryFactory {
    @Override
    public CommandRegistry createShellCommandRegistry(LookupContext lookupContext) {
        CommandLine cmd = new CommandLine(new CLI());
        PicocliCommands picocliCommands = new PicocliCommands(cmd);
        picocliCommands.name("Maveniverse Toolbox");
        return picocliCommands;
    }
}
