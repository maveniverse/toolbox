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
import eu.maveniverse.maven.toolbox.plugin.ContextMapAware;
import eu.maveniverse.maven.toolbox.plugin.CwdAware;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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
    // shared context for all mvnsh commands
    private final Map<Object, Object> contextMap = Collections.synchronizedMap(new HashMap<>());

    @Override
    public CommandRegistry createShellCommandRegistry(LookupContext lookupContext) {
        Runtimes.INSTANCE.registerRuntime(new StandaloneStaticRuntime());

        PicocliCommands.PicocliCommandsFactory factory = new PicocliCommands.PicocliCommandsFactory() {
            @Override
            public <K> K create(Class<K> clazz) throws Exception {
                K result = super.create(clazz);
                if (result instanceof CwdAware cwdAware) {
                    cwdAware.setCwd(lookupContext.cwd.get());
                }
                if (result instanceof ContextMapAware contextMapAware) {
                    contextMapAware.setContextMap(contextMap);
                }
                return result;
            }
        };
        factory.setTerminal(lookupContext.terminal);

        PicocliCommands picocliCommands = new PicocliCommands(new CommandLine(new CLI(), factory));
        picocliCommands.name("Maveniverse Toolbox");
        return picocliCommands;
    }
}
