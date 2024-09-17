/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.output;

import java.io.Closeable;
import java.util.function.Supplier;

/**
 * The output used for... output.
 */
public interface Output extends Closeable {
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

    Verbosity getVerbosity();

    boolean isShowErrors();

    <T> T tool(Class<T> klazz, Supplier<T> supplier);

    Marker marker(Verbosity verbosity);

    boolean isHeard(Verbosity verbosity);

    void doTell(String message, Object... params);

    void tell(String message, Object... params);

    void suggest(String message, Object... params);

    void chatter(String message, Object... params);

    void warn(String message, Object... params);

    void handle(Verbosity verbosity, String message, Object... params);
}
