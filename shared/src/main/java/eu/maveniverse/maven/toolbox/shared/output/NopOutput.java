/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.output;

/**
 * No op {@link Output}.
 */
public final class NopOutput extends OutputSupport {
    public static final NopOutput INSTANCE = new NopOutput();

    private NopOutput() {
        super(Verbosity.SILENT, false);
    }

    @Override
    protected void doHandle(Intent intent, Verbosity verbosity, String message, Object... params) {}
}
