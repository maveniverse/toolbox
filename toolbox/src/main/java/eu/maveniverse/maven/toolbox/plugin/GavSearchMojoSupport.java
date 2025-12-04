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

/**
 * Support class for "project unaware" search Mojos.
 */
public abstract class GavSearchMojoSupport extends GavMojoSupport {
    /**
     * A repository ID. Maybe a "well known" one, or if all repository data given, a new one.
     */
    @CommandLine.Option(
            names = {"--repositoryId"},
            description = "A repository ID. Maybe a 'well known' one, or if all repository data given, a new one")
    @Parameter(property = "repositoryId")
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
    @Parameter(property = "repositoryVendor", alias = "toolbox.search.backend.type")
    protected String repositoryVendor;

    protected RemoteRepository getRemoteRepository(ToolboxCommando toolboxCommando) {
        if (repositoryId != null) {
            RemoteRepository remoteRepository = toolboxCommando.remoteRepositories().stream()
                    .filter(r -> repositoryId.equals(r.getId()))
                    .findFirst()
                    .orElse(null);
            if (remoteRepository == null) {
                remoteRepository =
                        toolboxCommando.getKnownSearchRemoteRepositories().get(repositoryId);
            }
            if (remoteRepository != null) {
                return remoteRepository;
            }
        }
        if (repositoryId != null && repositoryBaseUri != null && repositoryVendor != null) {
            return toolboxCommando.parseRemoteRepository(
                    repositoryId + "::" + repositoryVendor + "::" + repositoryBaseUri);
        }
        if (!toolboxCommando.remoteRepositories().isEmpty()) {
            return toolboxCommando.remoteRepositories().get(0);
        } else {
            throw new IllegalArgumentException("Could not select usable remote repository");
        }
    }
}
