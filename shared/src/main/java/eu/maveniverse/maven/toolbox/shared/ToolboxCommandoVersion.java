/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

import static eu.maveniverse.maven.toolbox.shared.internal.ToolboxCommandoImpl.discoverArtifactVersion;

public final class ToolboxCommandoVersion {
    private ToolboxCommandoVersion() {}

    public static String getVersion() {
        return discoverArtifactVersion("eu.maveniverse.maven.toolbox", "shared", "unknown");
    }
}
