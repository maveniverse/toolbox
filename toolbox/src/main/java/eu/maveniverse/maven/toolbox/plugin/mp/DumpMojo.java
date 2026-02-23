/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.mp;

import eu.maveniverse.maven.toolbox.plugin.MPMojoSupport;
import eu.maveniverse.maven.toolbox.shared.Result;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import picocli.CommandLine;

/**
 * Dumps the MIMA environment.
 */
@Mojo(name = "dump", threadSafe = true)
public class DumpMojo extends MPMojoSupport {
    /**
     * Output it as Java Properties format.
     */
    @CommandLine.Option(
            names = {"--asProperties"},
            defaultValue = "false",
            description = "Output it as Java Properties format")
    @Parameter(property = "asProperties", defaultValue = "false", required = true)
    private boolean asProperties;

    /**
     * Output it into given file (only if asProperties=true).
     */
    @CommandLine.Option(
            names = {"--toFile"},
            description = "Output it into given file (only if asProperties=true)")
    @Parameter(property = "toFile")
    private File toFile;

    @Override
    protected Result<String> doExecute() throws IOException {
        if (asProperties) {
            Result<Map<String, String>> result = getToolboxCommando().dumpAsMap();
            Properties properties = new Properties();
            properties.putAll(result.getData().orElse(Collections.emptyMap()));
            if (toFile != null) {
                toFile.getParentFile().mkdirs();
                try (OutputStream fos = Files.newOutputStream(toFile.toPath())) {
                    properties.store(fos, "Toolbox dump");
                }
            } else {
                properties.store(System.out, "Toolbox dump");
            }
            return Result.success("success");
        } else {
            // dumps to console for human consumption
            return getToolboxCommando().dump();
        }
    }
}
