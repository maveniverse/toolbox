/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.toolbox.shared.LoggerOutput;
import eu.maveniverse.maven.toolbox.shared.Output;
import eu.maveniverse.maven.toolbox.shared.PrintStreamOutput;
import java.io.IOException;
import org.jline.jansi.Ansi;
import org.jline.terminal.TerminalBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for properly configured {@link Logger} instances to serve as "output".
 */
public final class OutputFactory {
    private OutputFactory() {}

    private static void dumpOutputStatus(Output output) {
        if (output.isHeard(Output.Verbosity.CHATTER)) {
            output.chatter("Using output {}", output.getClass().getSimpleName());
            output.chatter("Output verbosity '{}'", output.getVerbosity());
            output.chatter("ANSI detected={} and enabled={}", Ansi.isDetected(), Ansi.isEnabled());
        }
    }

    /**
     * When running as Maven plugin, we use {@link Logger} and Maven "drives" (Logging engine, ANSI, etc).
     */
    public static Output createMojoOutput(Output.Verbosity verbosity) {
        requireNonNull(verbosity, "verbosity");
        Output output = new LoggerOutput(LoggerFactory.getLogger(OutputFactory.class), verbosity);
        dumpOutputStatus(output);
        return output;
    }

    /**
     * When running as CLI, we need to set up ourselves fully.
     */
    public static Output createCliOutput(boolean batchMode, Output.Verbosity verbosity) {
        requireNonNull(verbosity, "verbosity");
        Output output;
        if (!batchMode && System.console() != null) {
            Ansi.setEnabled(true);
            try {
                output = new JLine3Output(verbosity, TerminalBuilder.builder().build());
            } catch (IOException e) {
                System.err.println("Error creating JLine3 Terminal; fallback to System.out");
                e.printStackTrace(System.err);
                output = new PrintStreamOutput(System.out, verbosity);
            }
        } else {
            output = new PrintStreamOutput(System.out, verbosity);
        }
        dumpOutputStatus(output);
        return output;
    }
}
