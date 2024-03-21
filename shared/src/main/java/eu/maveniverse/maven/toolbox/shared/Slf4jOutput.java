/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

import static java.util.Objects.requireNonNull;

import org.slf4j.Logger;

/**
 * Slf4j Logger backed Output.
 */
public final class Slf4jOutput implements Output {
    private final Logger logger;

    public Slf4jOutput(Logger logger) {
        this.logger = requireNonNull(logger, "logger");
    }

    @Override
    public boolean isVerbose() {
        return logger.isDebugEnabled();
    }

    @Override
    public void verbose(String msg, Object... params) {
        logger.debug(msg, params);
    }

    @Override
    public void normal(String msg, Object... params) {
        logger.info(msg, params);
    }

    @Override
    public void warn(String msg, Object... params) {
        logger.warn(msg, params);
    }

    @Override
    public void error(String msg, Object... params) {
        logger.error(msg, params);
    }
}
