/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

/**
 * Label for "build path", like "compile", "runtime", etc.
 */
public interface BuildPath {
    /**
     * The label.
     */
    String getId();

    /**
     * A flag denoting that this build path "reverses" the expected order.
     * <p>
     * For example: "compile" step expects "main" then "test" to be compiled. On the other
     * hand, "runtime" expects "test" to have run first. In this sense, "runtime" is reverse.
     */
    boolean isReverse();

    /**
     * Returns the "order" of this path, usable to sort against other instances.
     * Expected natural order is "compile", "runtime"... (basically like the processing order).
     */
    int order();
}
