/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.mvnsh;

import eu.maveniverse.maven.mima.context.Runtimes;
import eu.maveniverse.maven.mima.runtime.standalonestatic.StandaloneStaticRuntime;
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
        Runtimes.INSTANCE.registerRuntime(new StandaloneStaticRuntime());

        PicocliCommands.PicocliCommandsFactory factory = new PicocliCommands.PicocliCommandsFactory();
        factory.setTerminal(lookupContext.terminal);

        PicocliCommands picocliCommands = new PicocliCommands(new CommandLine(new CLI(), factory));
        picocliCommands.name("Maveniverse Toolbox");
        return picocliCommands;
    }
}
