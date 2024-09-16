/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.output;

import static java.util.Objects.requireNonNull;

/**
 * Simple Marker, useful to assemble single message line with different markings.
 */
public class Marker {
    private final Output output;
    private final Output.Verbosity verbosity;
    private final StringBuilder message;

    public Marker(Output output, Output.Verbosity verbosity) {
        this.output = output;
        this.verbosity = verbosity;
        this.message = new StringBuilder();
    }

    public Marker word(String word) {
        requireNonNull(word, "word");
        message.append(word);
        return this;
    }

    public Marker emphasize(String word) {
        return word(word);
    }

    public Marker outstanding(String word) {
        return word(word);
    }

    public Marker normal(String word) {
        return word(word);
    }

    public Marker detail(String word) {
        return word(word);
    }

    public Marker unimportant(String word) {
        return word(word);
    }

    public Marker scary(String word) {
        return word(word);
    }

    public void say(Object... params) {
        if (output.isHeard(verbosity)) {
            output.handle(verbosity, message.toString(), params);
        }
    }

    public String toString() {
        String result = message.toString();
        message.setLength(0);
        return result;
    }
}
