/*
 * Copyright (c) 2023-2024 Maveniverse Org.
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
    public NopOutput() {
        super(Verbosity.SILENT, false);
    }

    @Override
    public void handle(Verbosity verbosity, String message, Object... params) {}
}
