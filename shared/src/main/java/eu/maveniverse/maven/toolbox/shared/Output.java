/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

import java.util.function.Consumer;

/**
 * Output.
 */
public interface Output extends Consumer<String> {
    @Override
    default void accept(String s) {
        normal(s);
    }

    boolean isVerbose();

    void verbose(String msg, Object... params);

    void normal(String msg, Object... params);

    void error(String msg, Object... params);
}
