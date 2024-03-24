/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin;

import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.repository.RemoteRepository;
import picocli.CommandLine;

public abstract class GavSearchMojoSupport extends GavMojoSupport {
    /**
     * A repository ID. Maybe a "well known" one, or if all repository data given, a new one.
     */
    @CommandLine.Option(
            names = {"--repositoryId"},
            defaultValue = "central",
            description = "A repository ID. Maybe a 'well known' one, or if all repository data given, a new one")
    @Parameter(property = "repositoryId", defaultValue = "central")
    protected String repositoryId;

    /**
     * The base URI of the remote repository.
     */
    @CommandLine.Option(
            names = {"--repositoryBaseUri"},
            description = "The base URI of the remote repository")
    @Parameter(property = "repositoryBaseUri")
    protected String repositoryBaseUri;

    /**
     * The vendor of the remote repository.
     */
    @CommandLine.Option(
            names = {"--repositoryVendor"},
            description = "The vendor of the remote repository")
    @Parameter(property = "repositoryVendor")
    private String repositoryVendor;

    protected RemoteRepository getRemoteRepository(ToolboxCommando toolboxCommando) {
        RemoteRepository remoteRepository =
                toolboxCommando.getKnownSearchRemoteRepositories().get(repositoryId);
        if (remoteRepository != null) {
            return remoteRepository;
        }
        if (repositoryBaseUri == null && repositoryVendor == null) {
            throw new IllegalArgumentException("for new remote repository one must specify all information");
        }
        return toolboxCommando.parseRemoteRepository(repositoryId + "::" + repositoryVendor + "::" + repositoryBaseUri);
    }
}
