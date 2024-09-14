/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

import static java.util.Objects.requireNonNull;

/**
 * The output used for... output.
 */
public interface Output {
    enum Verbosity {
        /**
         * No output is emitted.
         */
        silent,
        /**
         * Tight-lipped: merely the "result" (whatever it is) is emitted.
         */
        tight,
        /**
         * Normal verbosity level.
         */
        normal,
        /**
         * More than normal verbosity level.
         */
        suggest,
        /**
         * Insane verbosity level.
         */
        chatter
    }

    default boolean isHeard(Verbosity verbosity) {
        requireNonNull(verbosity);
        return getVerbosity().ordinal() >= verbosity.ordinal();
    }

    Verbosity getVerbosity();

    void doTell(String message, Object... params);

    void tell(String message, Object... params);

    void suggest(String message, Object... params);

    void chatter(String message, Object... params);
}
