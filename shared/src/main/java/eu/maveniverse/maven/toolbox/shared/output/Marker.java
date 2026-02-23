/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.output;

import static java.util.Objects.requireNonNull;

/**
 * Simple "Marker (Pen)", useful to assemble single message line with different markings. This implementation is
 * "no op" (does not do anything), subclasses of it may do more.
 */
public class Marker {
    private final Output output;
    private final Output.Intent intent;
    private final Output.Verbosity verbosity;
    private final StringBuilder message;

    public Marker(Output output, Output.Intent intent, Output.Verbosity verbosity) {
        this.output = output;
        this.intent = intent;
        this.verbosity = verbosity;
        this.message = new StringBuilder();
    }

    public enum Intent {
        EMPHASIZE,
        OUTSTANDING,
        NORMAL,
        DETAIL,
        UNIMPORTANT,
        SCARY,
        BLOODY
    }

    public Marker word(Intent intent, String word) {
        requireNonNull(intent, "intent");
        requireNonNull(word, "word");
        message.append(word);
        return this;
    }

    public Marker emphasize(String word) {
        return word(Intent.EMPHASIZE, word);
    }

    public Marker outstanding(String word) {
        return word(Intent.OUTSTANDING, word);
    }

    public Marker normal(String word) {
        return word(Intent.NORMAL, word);
    }

    public Marker detail(String word) {
        return word(Intent.DETAIL, word);
    }

    public Marker unimportant(String word) {
        return word(Intent.UNIMPORTANT, word);
    }

    public Marker scary(String word) {
        return word(Intent.SCARY, word);
    }

    public Marker bloody(String word) {
        return word(Intent.BLOODY, word);
    }

    public void say(Object... params) {
        if (output.isHeard(verbosity)) {
            output.handle(intent, verbosity, toString(), params);
        }
    }

    @Override
    public String toString() {
        String result = message.toString();
        message.setLength(0);
        return result;
    }
}
