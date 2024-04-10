/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public abstract class GavMojoSupport extends MojoSupport {
    /**
     * Splits comma separated string into elements.
     */
    protected Collection<String> csv(String csv) {
        if (csv == null || csv.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(csv.split(","));
    }
    /**
     * Slurps, either comma separated string, or if value is existing file, will read
     * up the file with values on separate lines.
     */
    protected Collection<String> slurp(String csv) throws IOException {
        if (csv == null || csv.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            Path target = Paths.get(csv).toAbsolutePath();
            if (Files.isRegularFile(target) && Files.size(target) < 5_000_000) {
                return Files.readAllLines(target);
            }
        } catch (InvalidPathException e) {
            // ignore
        }
        return csv(csv);
    }
}
