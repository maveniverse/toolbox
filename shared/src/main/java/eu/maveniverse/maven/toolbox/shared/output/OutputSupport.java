/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.output;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.function.Supplier;

/**
 * The output support class.
 */
public abstract class OutputSupport implements Output {
    protected final Verbosity verbosity;
    protected final boolean errors;

    public OutputSupport(Verbosity verbosity, boolean errors) {
        this.verbosity = requireNonNull(verbosity, "verbosity");
        this.errors = errors;
    }

    @Override
    public Verbosity getVerbosity() {
        return verbosity;
    }

    @Override
    public boolean isShowErrors() {
        return errors;
    }

    @Override
    public void close() throws IOException {}

    public <T> T tool(Class<? extends T> klazz, Supplier<T> supplier) {
        requireNonNull(supplier, "supplier");
        return supplier.get();
    }

    public Marker marker(Verbosity verbosity) {
        return new Marker(this, verbosity);
    }

    public boolean isHeard(Verbosity verbosity) {
        requireNonNull(verbosity);
        return getVerbosity().ordinal() >= verbosity.ordinal();
    }

    public void doTell(String message, Object... params) {
        if (isHeard(Verbosity.TIGHT)) {
            handle(Verbosity.TIGHT, message, params);
        }
    }

    public void tell(String message, Object... params) {
        if (isHeard(Verbosity.NORMAL)) {
            handle(Verbosity.NORMAL, message, params);
        }
    }

    public void suggest(String message, Object... params) {
        if (isHeard(Verbosity.SUGGEST)) {
            handle(Verbosity.SUGGEST, message, params);
        }
    }

    public void chatter(String message, Object... params) {
        if (isHeard(Verbosity.CHATTER)) {
            handle(Verbosity.CHATTER, message, params);
        }
    }

    public void warn(String message, Object... params) {
        new Marker(this, Verbosity.TIGHT).scary("[W] " + message).say(params);
    }

    public void error(String message, Object... params) {
        new Marker(this, Verbosity.TIGHT).bloody("[E] " + message).say(params);
    }

    @Override
    public void handle(Verbosity verbosity, String message, Object... params) {
        if (isHeard(verbosity)) {
            doHandle(verbosity, message, params);
        }
    }

    protected abstract void doHandle(Verbosity verbosity, String message, Object... params);
}
