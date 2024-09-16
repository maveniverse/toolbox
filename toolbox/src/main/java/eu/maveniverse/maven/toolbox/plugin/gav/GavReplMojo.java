/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.gav;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.toolbox.plugin.CLI;
import eu.maveniverse.maven.toolbox.plugin.GavMojoSupport;
import eu.maveniverse.maven.toolbox.shared.Result;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import eu.maveniverse.maven.toolbox.shared.output.Output;
import java.nio.file.Path;
import org.apache.maven.plugins.annotations.Mojo;
import org.jline.builtins.ConfigurationPath;
import org.jline.console.SystemRegistry;
import org.jline.console.impl.Builtins;
import org.jline.console.impl.SystemRegistryImpl;
import org.jline.keymap.KeyMap;
import org.jline.reader.Binding;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.MaskingCallback;
import org.jline.reader.Parser;
import org.jline.reader.Reference;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.impl.jni.JniTerminalProvider;
import org.jline.widget.TailTipWidgets;
import picocli.CommandLine;
import picocli.shell.jline3.PicocliCommands;

@CommandLine.Command(name = "repl", description = "REPL console")
@Mojo(name = "gav-repl", requiresProject = false, threadSafe = true)
public class GavReplMojo extends GavMojoSupport {
    @Override
    public Result<String> doExecute(Output output, ToolboxCommando toolboxCommando) {
        Class<?> tp = JniTerminalProvider.class;
        Context context = getContext();

        toolboxCommando.dump(output);

        // set up JLine built-in commands
        ConfigurationPath configPath = new ConfigurationPath(context.basedir(), context.basedir());
        Builtins builtins = new Builtins(context::basedir, configPath, null);
        builtins.rename(Builtins.Command.TTOP, "top");
        builtins.alias("zle", "widget");
        builtins.alias("bindkey", "keymap");
        // set up picocli commands
        CommandLine cmd = new CommandLine(new CLI());
        PicocliCommands picocliCommands = new PicocliCommands(cmd);

        Parser parser = new DefaultParser();

        try (Terminal terminal = TerminalBuilder.builder().name("mima").build()) {
            SystemRegistry systemRegistry = new SystemRegistryImpl(parser, terminal, context::basedir, configPath);
            systemRegistry.setCommandRegistries(builtins, picocliCommands);

            Path history = context.mavenUserHome().basedir().resolve(".mima_history");
            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .history(new DefaultHistory())
                    .completer(systemRegistry.completer())
                    .parser(parser)
                    .variable(LineReader.LIST_MAX, 50) // max tab completion candidates
                    .variable(LineReader.HISTORY_FILE, history)
                    .build();
            builtins.setLineReader(reader);
            new TailTipWidgets(reader, systemRegistry::commandDescription, 5, TailTipWidgets.TipType.COMPLETER);
            KeyMap<Binding> keyMap = reader.getKeyMaps().get("main");
            keyMap.bind(new Reference("tailtip-toggle"), KeyMap.alt("s"));

            String prompt = "prompt> ";
            String rightPrompt = null;

            // start the shell and process input until the user quits with Ctrl-D
            String line;
            while (true) {
                try {
                    systemRegistry.cleanUp();
                    line = reader.readLine(prompt, rightPrompt, (MaskingCallback) null, null);
                    systemRegistry.execute(line);
                } catch (UserInterruptException e) {
                    // Ignore
                } catch (EndOfFileException e) {
                    return Result.success("eof");
                } catch (SystemRegistryImpl.UnknownCommandException e) {
                    output.doTell("REPL Failure: ", e);
                } catch (Exception e) {
                    systemRegistry.trace(e);
                    return Result.failure(e.getMessage());
                }
            }
        } catch (Exception e) {
            output.doTell("REPL Failure: ", e);
            return Result.failure(e.getMessage());
        }
    }
}
