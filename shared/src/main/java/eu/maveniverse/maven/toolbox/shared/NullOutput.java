/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

/**
 * Null Output.
 */
public final class NullOutput implements Output {
    public NullOutput() {}

    @Override
    public boolean isVerbose() {
        return false;
    }

    @Override
    public void verbose(String msg, Object... params) {}

    @Override
    public void normal(String msg, Object... params) {}

    @Override
    public void warn(String msg, Object... params) {}

    @Override
    public void error(String msg, Object... params) {}
}
