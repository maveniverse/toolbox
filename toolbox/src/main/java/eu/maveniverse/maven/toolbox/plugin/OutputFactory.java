/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for properly configured {@link Logger} instances to serve as "output".
 */
public final class OutputFactory {
    private OutputFactory() {}

    public enum Verbosity {
        silent,
        normal,
        high,
        insane
    }

    private static class Silent {}

    private static class Normal {}

    private static class High {}

    private static class Insane {}

    public static Logger createOutput(Verbosity verbosity) {
        return switch (verbosity) {
            case silent -> LoggerFactory.getLogger(Silent.class);
            case normal -> LoggerFactory.getLogger(Normal.class);
            case high -> LoggerFactory.getLogger(High.class);
            case insane -> LoggerFactory.getLogger(Insane.class);
        };
    }
}
