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
        SILENT,
        /**
         * Tight-lipped: merely the "result" (whatever it is) is emitted.
         */
        TIGHT,
        /**
         * Normal verbosity level.
         */
        NORMAL,
        /**
         * More than normal verbosity level.
         */
        SUGGEST,
        /**
         * Insane verbosity level.
         */
        CHATTER
    }

    default boolean isHeard(Verbosity verbosity) {
        requireNonNull(verbosity);
        return getVerbosity().ordinal() >= verbosity.ordinal();
    }

    Verbosity getVerbosity();

    default void doTell(String message, Object... params) {
        if (isHeard(Verbosity.TIGHT)) {
            handle(Verbosity.TIGHT, message, params);
        }
    }

    default void tell(String message, Object... params) {
        if (isHeard(Verbosity.NORMAL)) {
            handle(Verbosity.NORMAL, message, params);
        }
    }

    default void suggest(String message, Object... params) {
        if (isHeard(Verbosity.SUGGEST)) {
            handle(Verbosity.SUGGEST, message, params);
        }
    }

    default void chatter(String message, Object... params) {
        if (isHeard(Verbosity.CHATTER)) {
            handle(Verbosity.CHATTER, message, params);
        }
    }

    void handle(Verbosity verbosity, String message, Object... params);
}
