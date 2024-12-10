/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.gav;

import eu.maveniverse.maven.toolbox.plugin.GavMojoSupport;
import eu.maveniverse.maven.toolbox.shared.Result;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import java.nio.file.Path;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.metadata.DefaultMetadata;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.repository.RemoteRepository;
import picocli.CommandLine;

/**
 * Prints expected relative path for a given Maven Metadata in a local repository.
 */
@CommandLine.Command(
        name = "metadata-path",
        description = "Prints expected relative path for a given Maven Metadata in a local repository")
@Mojo(name = "gav-metadata-path", requiresProject = false, threadSafe = true)
public class GavMetadataPathMojo extends GavMojoSupport {
    /**
     * The metadata coordinates in form of {@code [G]:[A]:[V]:[type]}. Absence of {@code A} implies absence of {@code V}
     * as well (in other words, it can be {@code G}, {@code G:A} or {@code G:A:V}). The absence of {@code type} implies
     * it is "maven-metadata.xml". The simplest spec string is {@code :::}.
     * <p>
     * Examples:
     * <ul>
     *     <li>{@code :::} is root metadata named "maven-metadata.xml"</li>
     *     <li>{@code :::my-metadata.xml} is root metadata named "my-metadata.xml"</li>
     *     <li>{@code G:::} equals to {@code G:::maven-metadata.xml}</li>
     *     <li>{@code G:A::} equals to {@code G:A::maven-metadata.xml}</li>
     * </ul>
     */
    @CommandLine.Parameters(index = "0", description = "The Metadata coordinates", arity = "1")
    @Parameter(property = "gav", required = true)
    private String gav;

    /**
     * The optional remote repository spec string. It is expected to be in form of {@code id::url}, but we are really
     * interested in repository ID only.
     */
    @CommandLine.Option(
            names = {"--repository"},
            description =
                    "The optional remote repository spec string. It is expected to be in form of {@code id::url}, but we are really interested in repository ID only.")
    @Parameter(property = "repository")
    private String repository;

    @Override
    protected Result<Path> doExecute() throws Exception {
        ToolboxCommando toolboxCommando = getToolboxCommando();
        RemoteRepository rr = null;
        if (repository != null) {
            rr = toolboxCommando.parseRemoteRepository(repository);
        }

        String groupId = null;
        String artifactId = null;
        String version = null;
        String type = "maven-metadata.xml";
        String[] elems = gav.split(":", 0);
        if (elems.length > 0) {
            groupId = elems[0];
        }
        if (elems.length > 1) {
            artifactId = elems[1];
        }
        if (elems.length > 2) {
            version = elems[2];
        }
        if (elems.length > 3) {
            type = elems[3];
        }
        if (elems.length > 4) {
            throw new IllegalArgumentException("Invalid gav: " + gav);
        }
        return toolboxCommando.metadataPath(
                new DefaultMetadata(groupId, artifactId, version, type, Metadata.Nature.RELEASE_OR_SNAPSHOT), rr);
    }
}
