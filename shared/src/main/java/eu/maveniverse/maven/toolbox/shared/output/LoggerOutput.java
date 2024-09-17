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
public final class LoggerOutput extends OutputSupport {
    private final Logger output;

    public LoggerOutput(Logger output, Verbosity verbosity) {
        super(verbosity, true);
        this.output = output;
    }

    @Override
    public void warn(String message, Object... params) {
        output.warn(message, params);
    }

    @Override
    public void error(String message, Object... params) {
        output.error(message, params);
    }

    @Override
    protected void doHandle(Verbosity verbosity, String message, Object... params) {
        output.info(message, params);
    }
}
