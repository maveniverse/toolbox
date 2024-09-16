/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.output;

import org.slf4j.Logger;

/**
 * {@link Output} backed with Slf4J {@link Logger}.
 */
public final class LoggerOutput implements Output {
    private final Logger output;
    private final Verbosity verbosity;

    public LoggerOutput(Logger output, Verbosity verbosity) {
        this.output = output;
        this.verbosity = verbosity;
    }

    @Override
    public Verbosity getVerbosity() {
        return verbosity;
    }

    @Override
    public void handle(Verbosity verbosity, String message, Object... params) {
        output.info(message, params);
    }
}
