/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.cli;

import org.eclipse.aether.repository.RemoteRepository;
import picocli.CommandLine;

/**
 * Search support.
 */
public abstract class SearchCommandSupport extends CommandSupport {
    @CommandLine.Option(
            names = {"--repositoryId"},
            defaultValue = "central",
            description = "The targeted repository ID")
    protected String repositoryId;

    @CommandLine.Option(
            names = {"--repositoryBaseUri"},
            defaultValue = "https://repo.maven.apache.org/maven2/",
            description = "The targeted repository base Uri")
    protected String repositoryBaseUri;

    @CommandLine.Option(
            names = {"--repositoryVendor"},
            defaultValue = "central",
            description = "The targeted repository vendor")
    protected String repositoryVendor;

    protected RemoteRepository getRemoteRepository() {
        return new RemoteRepository.Builder(repositoryId, "default", repositoryBaseUri).build();
    }
}
