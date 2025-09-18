/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.util.Optional;

public interface CwdAware {
    /**
     * Sets CWD; must be non-null path that points to existing directory.
     */
    void setCwd(Path cwd);

    /**
     * Returns CWD, if set; a non-null path that points to existing directory.
     */
    Optional<Path> getCwd();

    /**
     * Resolves against CWD, if set.
     */
    default Path resolve(Path path) {
        requireNonNull(path, "path");
        return getCwd().orElseThrow().resolve(path).normalize().toAbsolutePath();
    }
}
