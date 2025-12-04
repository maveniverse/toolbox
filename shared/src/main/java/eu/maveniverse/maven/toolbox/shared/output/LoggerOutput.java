/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.output;

import java.util.Arrays;
import org.slf4j.Logger;

/**
 * {@link Output} backed with Slf4J {@link Logger}.
 */
public final class LoggerOutput extends OutputSupport {
    private final Logger output;

    public LoggerOutput(Logger output, boolean errors, Verbosity verbosity) {
        super(verbosity, errors);
        this.output = output;
    }

    @Override
    public void warn(String message, Object... params) {
        if (!isShowErrors() && params.length > 0 && params[params.length - 1] instanceof Throwable) {
            output.warn(
                    message + " " + ((Throwable) params[params.length - 1]).getMessage(),
                    Arrays.copyOf(params, params.length - 1));
        } else {
            output.warn(message, params);
        }
    }

    @Override
    public void error(String message, Object... params) {
        if (!isShowErrors() && params.length > 0 && params[params.length - 1] instanceof Throwable) {
            output.error(
                    message + " " + ((Throwable) params[params.length - 1]).getMessage(),
                    Arrays.copyOf(params, params.length - 1));
        } else {
            output.error(message, params);
        }
    }

    @Override
    protected void doHandle(Intent intent, Verbosity verbosity, String message, Object... params) {
        if (intent == Intent.OUT) {
            output.info(message, params);
        } else {
            output.warn(message, params);
        }
    }
}
