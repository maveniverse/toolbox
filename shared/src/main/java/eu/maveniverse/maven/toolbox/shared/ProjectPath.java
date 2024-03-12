/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

/**
 * Label for "project path", like "main", "test", but could be "it", etc.
 */
public interface ProjectPath {
    /**
     * The label.
     */
    String getId();

    /**
     * Returns the "order" of this path, usable to sort against other instances.
     * Expected natural order is "main", "test"... (basically like the processing order).
     */
    int order();
}
