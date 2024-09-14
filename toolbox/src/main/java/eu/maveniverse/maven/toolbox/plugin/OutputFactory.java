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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for properly configured {@link Logger} instances to serve as "output".
 */
public final class OutputFactory {
    private OutputFactory() {}

    public static Output createMojoOutput(Output.Verbosity verbosity) {
        requireNonNull(verbosity, "verbosity");
        return new LoggerOutput(LoggerFactory.getLogger(OutputFactory.class), verbosity);
    }

    public static Output createCliOutput(Output.Verbosity verbosity) {
        requireNonNull(verbosity, "verbosity");
        return new PrintStreamOutput(System.out, verbosity);
    }
}
