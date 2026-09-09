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
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import java.io.File;
import java.util.Map;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Downloads a file from remote using Maven settings.
 */
@Mojo(name = "http-get", threadSafe = true)
public class HttpGetMojo extends MPMojoSupport {
    private static final Logger log = LoggerFactory.getLogger(HttpGetMojo.class);
    /**
     * The HTTP URL of the remote resource to download. It can be:
     * <ul>
     *     <li>URL</li>
     *     <li>serverId::URL</li>
     * </ul>
     * In latter case, it will pick up auth and all Maven specific config for given server ID.
     */
    @Parameter(property = "remoteResource", required = true)
    private String remoteResource;

    /**
     * Should the downloaded (archive) file be unpacked?
     */
    @Parameter(property = "unpack", defaultValue = "false", required = true)
    private boolean unpack;

    /**
     * When unpacking, should the archive root entry be used or skipped?
     */
    @Parameter(property = "useRoot", defaultValue = "true", required = true)
    private boolean useRoot;

    /**
     * Optionally, to enforce Resolver supported checksums on resource (the origin, in case unpack=true).
     */
    @Parameter(property = "checksums")
    private Map<String, String> checksums;

    /**
     * The output file (or directory, when unpack) to store the resource.
     */
    @Parameter(property = "output", defaultValue = "${project.build.directory}/http-get", required = true)
    private File output;

    @Override
    protected Result<String> doExecute() throws Exception {
        ToolboxCommando toolboxCommando = getToolboxCommando();
        if (unpack) {
            log.info("Downloading {} and unpacking it to {} directory", remoteResource, output);
        } else {
            log.info("Downloading {} to {} directory", remoteResource, output);
        }
        return toolboxCommando.httpGet(
                toolboxCommando.parseRemoteRepository(remoteResource),
                useRoot,
                unpack,
                output.toPath(),
                checksums != null ? checksums : Map.of());
    }
}
