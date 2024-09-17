/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.output;

import java.io.PrintStream;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

/**
 * {@link Output} backed with {@link PrintStream}.
 */
public class PrintStreamOutput extends OutputSupport {
    protected final PrintStream output;

    public PrintStreamOutput(PrintStream output, Verbosity verbosity, boolean errors) {
        super(verbosity, errors);
        this.output = output;
    }

    @Override
    protected void doHandle(Verbosity verbosity, String message, Object... params) {
        FormattingTuple tuple = MessageFormatter.arrayFormat(message, params);
        output.println(tuple.getMessage());
        if (tuple.getThrowable() != null) {
            if (errors) {
                tuple.getThrowable().printStackTrace(output);
            } else {
                output.println(tuple.getThrowable().getMessage());
            }
        }
    }
}
