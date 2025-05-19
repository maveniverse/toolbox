/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.plugin.mp;

import eu.maveniverse.maven.toolbox.plugin.MPMojoSupport;
import eu.maveniverse.maven.toolbox.shared.ArtifactVersionMatcher;
import eu.maveniverse.maven.toolbox.shared.ArtifactVersionSelector;
import eu.maveniverse.maven.toolbox.shared.Result;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.version.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lists available core extension versions of Maven.
 */
@Mojo(name = "core-extension-versions", requiresProject = false, threadSafe = true)
public class CoreExtensionVersionsMojo extends MPMojoSupport {
    private static final Logger log = LoggerFactory.getLogger(CoreExtensionVersionsMojo.class);
    /**
     * Artifact version matcher spec string, default is 'noSnapshotsAndPreviews()'.
     */
    @Parameter(property = "artifactVersionMatcherSpec", defaultValue = "noSnapshotsAndPreviews()")
    private String artifactVersionMatcherSpec;

    /**
     * Artifact version selector spec string to select the version from candidates, default is 'last()'.
     */
    @Parameter(property = "artifactVersionSelectorSpec", defaultValue = "last()")
    private String artifactVersionSelectorSpec;

    /**
     * The scope where to look for core extensions ("project", "user" or "install").
     */
    @Parameter(property = "scope", defaultValue = "project")
    private String scope;

    /**
     * Apply results to extensions file.
     */
    @Parameter(property = "apply")
    private boolean apply;

    @Override
    protected Result<Boolean> doExecute() throws Exception {
        ToolboxCommando toolboxCommando = getToolboxCommando();
        ArtifactVersionMatcher artifactVersionMatcher =
                toolboxCommando.parseArtifactVersionMatcherSpec(artifactVersionMatcherSpec);
        ArtifactVersionSelector artifactVersionSelector =
                toolboxCommando.parseArtifactVersionSelectorSpec(artifactVersionSelectorSpec);

        ToolboxCommando.ExtensionsScope extensionsScope =
                ToolboxCommando.ExtensionsScope.valueOf(scope.toUpperCase(Locale.ROOT));
        Path extensionsXml;
        if (extensionsScope == ToolboxCommando.ExtensionsScope.PROJECT) {
            if (mavenSession.getRequest().isProjectPresent()) {
                extensionsXml = mavenSession
                        .getTopLevelProject()
                        .getBasedir()
                        .toPath()
                        .resolve(".mvn")
                        .resolve("extensions.xml");
            } else {
                extensionsXml = Path.of(".mvn").resolve("extensions.xml");
            }
        } else {
            extensionsXml = toolboxCommando.extensionsPath(extensionsScope);
        }
        Result<List<Artifact>> extensionsResult = toolboxCommando.listExtensions(extensionsXml);
        if (!extensionsResult.isSuccess()) {
            log.warn(
                    "Failed to list extensions from scope {} (path {}): {}",
                    extensionsScope,
                    extensionsXml,
                    extensionsResult.getMessage());
            return Result.failure(extensionsResult.getMessage());
        }

        Result<Map<Artifact, List<Version>>> extensions = toolboxCommando.versions(
                extensionsScope.name().toLowerCase(Locale.ROOT) + " extensions",
                () -> extensionsResult.getData().orElseThrow().stream(),
                artifactVersionMatcher,
                artifactVersionSelector);

        if (apply) {
            List<Artifact> extensionsUpdates =
                    toolboxCommando.calculateUpdates(extensions.getData().orElseThrow(), artifactVersionSelector);
            if (!extensionsUpdates.isEmpty()) {
                try (ToolboxCommando.EditSession editSession = toolboxCommando.createEditSession(extensionsXml)) {
                    toolboxCommando.editExtensions(editSession, ToolboxCommando.Op.UPDATE, extensionsUpdates::stream);
                }
            }
        }
        return Result.success(true);
    }
}
