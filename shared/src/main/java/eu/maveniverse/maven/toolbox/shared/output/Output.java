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
    /**
     * The verbosity level of output.
     */
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

    /**
     * The effective verbosity level of this output.
     */
    Verbosity getVerbosity();

    /**
     * The effective "show errors" state of this output.
     */
    boolean isShowErrors();

    /**
     * Get a "tool" from output. This method may return "improved" tool than the one supplier would create.
     */
    <T> T tool(Class<? extends T> klazz, Supplier<T> supplier);

    /**
     * Returns "text marker" to build simple messages.
     */
    Marker marker(Verbosity verbosity);

    /**
     * Returns {@code true} if given verbosity would be emitted according to "effective" verbosity of this output.
     *
     * @see #getVerbosity()
     */
    boolean isHeard(Verbosity verbosity);

    /**
     * Emits message at {@link Verbosity#TIGHT} verbosity. Result messages (most 1 or 2) should be emitted here,
     * that are the result.
     */
    void doTell(String message, Object... params);

    /**
     * Emits message at {@link Verbosity#NORMAL} verbosity. Normal messages should be emitted here,
     * probably describing the result.
     */
    void tell(String message, Object... params);

    /**
     * Emits message at {@link Verbosity#SUGGEST} verbosity. Detailed messages should be emitted here,
     * that user is not always interested in.
     */
    void suggest(String message, Object... params);

    /**
     * Emits message at {@link Verbosity#CHATTER} verbosity. A very detailed messages should be emitted here,
     * that user is usually not interested in.
     */
    void chatter(String message, Object... params);

    /**
     * Emits message that show "warning" to  user: operation is not aborted, but may hit some issues down the road.
     */
    void warn(String message, Object... params);

    /**
     * Emits message that show "error" to  user: operation is aborted with message explaining why.
     */
    void error(String message, Object... params);

    /**
     * Handles message at given verbosity.
     */
    void handle(Verbosity verbosity, String message, Object... params);
}
