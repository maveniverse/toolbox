/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.toolbox.shared.output.AnsiOutput;
import eu.maveniverse.maven.toolbox.shared.output.LoggerOutput;
import eu.maveniverse.maven.toolbox.shared.output.Output;
import eu.maveniverse.maven.toolbox.shared.output.PrintStreamOutput;
import org.jline.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for {@link Output}.
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
     * When running as Mojo, Maven set up {@link Logger} for us.
     */
    public static Output createMojoOutput(boolean batchMode, boolean errors, Output.Verbosity verbosity) {
        requireNonNull(verbosity, "verbosity");
        Output output = new LoggerOutput(LoggerFactory.getLogger(OutputFactory.class), verbosity);
        if (!batchMode && System.console() != null) {
            if (!Ansi.isEnabled()) {
                Ansi.setEnabled(true);
            }
            output = new AnsiOutput(output);
        }
        dumpOutputStatus(output);
        return output;
    }

    /**
     * When running as CLI, we need to set up ourselves fully.
     */
    public static Output createCliOutput(boolean batchMode, boolean errors, Output.Verbosity verbosity) {
        requireNonNull(verbosity, "verbosity");
        Output output = new PrintStreamOutput(System.out, verbosity, errors);
        if (!batchMode && System.console() != null) {
            if (!Ansi.isEnabled()) {
                Ansi.setEnabled(true);
            }
            output = new AnsiOutput(output);
        }
        dumpOutputStatus(output);
        return output;
    }
}
