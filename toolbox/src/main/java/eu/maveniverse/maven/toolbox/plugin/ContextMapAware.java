/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin;

import java.util.Map;
import java.util.Optional;

public interface ContextMapAware {
    /**
     * Sets context. The map must be non-null.
     */
    void setContextMap(Map<Object, Object> context);

    /**
     * Returns context, if set.
     */
    Optional<Map<Object, Object>> getContextMap();
}
