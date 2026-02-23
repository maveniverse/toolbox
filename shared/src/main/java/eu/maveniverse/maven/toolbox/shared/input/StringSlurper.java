/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.input;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * Input handling helper.
 */
public final class StringSlurper {
    private StringSlurper() {}

    /**
     * Splits comma separated string into elements.
     */
    public static Collection<String> csv(String csv) {
        if (csv == null || csv.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(csv.split("[,;|]"));
    }

    /**
     * Slurps, either comma separated string, or if value is existing file, will read
     * up the file with values on separate lines.
     */
    public static Collection<String> slurp(String csv) throws IOException {
        if (csv == null || csv.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            Path target = Path.of(csv).toAbsolutePath();
            if (Files.isRegularFile(target) && Files.size(target) < 5_000_000) {
                return Files.readAllLines(target);
            }
        } catch (InvalidPathException e) {
            // ignore
        }
        return csv(csv);
    }
}
