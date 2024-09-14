/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

import java.io.PrintStream;
import org.slf4j.helpers.MessageFormatter;

/**
 * {@link Output} backed with {@link PrintStream}.
 */
public final class PrintStreamOutput implements Output {
    private final PrintStream output;
    private final Verbosity verbosity;

    public PrintStreamOutput(PrintStream output, Verbosity verbosity) {
        this.output = output;
        this.verbosity = verbosity;
    }

    @Override
    public Verbosity getVerbosity() {
        return verbosity;
    }

    @Override
    public void doTell(String message, Object... params) {
        if (isHeard(Verbosity.tight)) {
            output.println(MessageFormatter.arrayFormat(message, params));
        }
    }

    @Override
    public void tell(String message, Object... params) {
        if (isHeard(Verbosity.normal)) {
            output.println(MessageFormatter.arrayFormat(message, params));
        }
    }

    @Override
    public void suggest(String message, Object... params) {
        if (isHeard(Verbosity.suggest)) {
            output.println(MessageFormatter.arrayFormat(message, params));
        }
    }

    @Override
    public void chatter(String message, Object... params) {
        if (isHeard(Verbosity.chatter)) {
            output.println(MessageFormatter.arrayFormat(message, params));
        }
    }
}
