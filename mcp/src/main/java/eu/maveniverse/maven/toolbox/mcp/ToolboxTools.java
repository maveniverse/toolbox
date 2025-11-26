/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.mcp;

import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.Runtimes;
import eu.maveniverse.maven.toolbox.shared.DependencyMatcher;
import eu.maveniverse.maven.toolbox.shared.ResolutionRoot;
import eu.maveniverse.maven.toolbox.shared.ResolutionScope;
import eu.maveniverse.maven.toolbox.shared.ToolboxCommando;
import eu.maveniverse.maven.toolbox.shared.internal.ArtifactSinks;
import eu.maveniverse.maven.toolbox.shared.internal.ArtifactSources;
import eu.maveniverse.maven.toolbox.shared.internal.ToolboxCommandoImpl;
import eu.maveniverse.maven.toolbox.shared.output.MarkdownOutput;
import eu.maveniverse.maven.toolbox.shared.output.Output;
import eu.maveniverse.maven.toolbox.shared.output.PrintStreamOutput;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.eclipse.aether.artifact.DefaultArtifact;

@ApplicationScoped
public class ToolboxTools {

    private ContextOverrides createCLIContextOverrides() {
        // create builder with some sane defaults
        return ContextOverrides.create().withUserSettings(true).build();
    }

    private ToolboxCommandoImpl createToolboxCommando(OutputStream output) {
        return (ToolboxCommandoImpl) ToolboxCommando.create(
                new MarkdownOutput(new PrintStreamOutput(new PrintStream(output), Output.Verbosity.NORMAL, false)),
                Runtimes.INSTANCE.getRuntime().create(createCLIContextOverrides()));
    }

    @Tool(description = "Get the Maven Artifact basic properties.")
    String artifactDescription(@ToolArg(description = "The artifact as groupId:artifactId:version") String gav) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ToolboxCommandoImpl toolboxCommando = createToolboxCommando(outputStream)) {
            toolboxCommando.copy(
                    ArtifactSources.gavArtifactSource(gav),
                    ArtifactSinks.statArtifactSink(0, true, true, toolboxCommando.output(), toolboxCommando));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return outputStream.toString(StandardCharsets.UTF_8);
    }

    @Tool(description = "Get the Maven Artifact classpath constituents and their description.")
    String artifactClasspathDescription(
            @ToolArg(description = "The artifact as groupId:artifactId:version") String gav) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ToolboxCommandoImpl toolboxCommando = createToolboxCommando(outputStream)) {
            toolboxCommando.copy(
                    ArtifactSources.resolveTransitiveArtifactSource(
                            ArtifactSources.gavArtifactSource(gav), toolboxCommando, ResolutionScope.RUNTIME),
                    ArtifactSinks.statArtifactSink(0, true, true, toolboxCommando.output(), toolboxCommando));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return outputStream.toString(StandardCharsets.UTF_8);
    }

    @Tool(description = "Get the Maven Artifact dependency tree.")
    String artifactDependencyTree(@ToolArg(description = "The artifact as groupId:artifactId:version") String gav) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ToolboxCommandoImpl toolboxCommando = createToolboxCommando(outputStream)) {
            toolboxCommando.tree(
                    ResolutionScope.RUNTIME,
                    ResolutionRoot.ofLoaded(new DefaultArtifact(gav)).build(),
                    true,
                    true,
                    DependencyMatcher.any());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return outputStream.toString(StandardCharsets.UTF_8);
    }
}
