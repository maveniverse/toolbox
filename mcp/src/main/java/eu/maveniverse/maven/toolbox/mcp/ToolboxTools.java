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
import eu.maveniverse.maven.toolbox.shared.ArtifactKeyFactory;
import eu.maveniverse.maven.toolbox.shared.ArtifactVersionMatcher;
import eu.maveniverse.maven.toolbox.shared.ArtifactVersionSelector;
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
                new MarkdownOutput(
                        new PrintStreamOutput(new PrintStream(output), System.err, Output.Verbosity.SUGGEST, false)),
                Runtimes.INSTANCE.getRuntime().create(createCLIContextOverrides()));
    }

    @Tool(description = "Check for Maven Artifact existence.")
    String artifactExists(@ToolArg(description = "The artifact as groupId:artifactId:version") String gav) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ToolboxCommandoImpl toolboxCommando = createToolboxCommando(outputStream)) {
            toolboxCommando.exists(ContextOverrides.CENTRAL, gav, true, true, true, true, false, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return outputStream.toString(StandardCharsets.UTF_8);
    }

    @Tool(description = "Get the Maven Artifact any newer versions than the one specified.")
    String artifactNewerAnyVersions(@ToolArg(description = "The artifact as groupId:artifactId:version") String gav) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ToolboxCommandoImpl toolboxCommando = createToolboxCommando(outputStream)) {
            toolboxCommando.versions(
                    "artifact",
                    ArtifactSources.gavArtifactSource(gav),
                    ArtifactVersionMatcher.any(),
                    ArtifactVersionSelector.contextualSnapshotsAndPreviews(ArtifactVersionSelector.last()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return outputStream.toString(StandardCharsets.UTF_8);
    }

    @Tool(description = "Get the Maven Artifact same major newer versions than the one specified.")
    String artifactNewerMajorVersions(@ToolArg(description = "The artifact as groupId:artifactId:version") String gav) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ToolboxCommandoImpl toolboxCommando = createToolboxCommando(outputStream)) {
            toolboxCommando.versions(
                    "artifact",
                    ArtifactSources.gavArtifactSource(gav),
                    ArtifactVersionMatcher.any(),
                    ArtifactVersionSelector.contextualSnapshotsAndPreviews(ArtifactVersionSelector.major()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return outputStream.toString(StandardCharsets.UTF_8);
    }

    @Tool(description = "Get the Maven Artifact basic properties.")
    String artifactBasicProperties(@ToolArg(description = "The artifact as groupId:artifactId:version") String gav) {
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

    @Tool(description = "Search for Maven artifacts by expression (supports query syntax like groupId:artifactId).")
    String artifactSearch(
            @ToolArg(description = "Search expression (e.g., 'g:org.springframework a:spring-core')")
                    String expression) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ToolboxCommandoImpl toolboxCommando = createToolboxCommando(outputStream)) {
            toolboxCommando.search(ContextOverrides.CENTRAL, expression);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return outputStream.toString(StandardCharsets.UTF_8);
    }

    @Tool(description = "Identify Maven artifact by file SHA-1 hash.")
    String artifactIdentify(@ToolArg(description = "SHA-1 hash of the artifact file") String sha1) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ToolboxCommandoImpl toolboxCommando = createToolboxCommando(outputStream)) {
            toolboxCommando.identify(ContextOverrides.CENTRAL, java.util.Collections.singleton(sha1), true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return outputStream.toString(StandardCharsets.UTF_8);
    }

    @Tool(description = "List available versions or artifacts for a given groupId:artifactId or groupId.")
    String artifactList(@ToolArg(description = "GroupId or groupId:artifactId to list") String gavoid) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ToolboxCommandoImpl toolboxCommando = createToolboxCommando(outputStream)) {
            toolboxCommando.list(ContextOverrides.CENTRAL, gavoid, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return outputStream.toString(StandardCharsets.UTF_8);
    }

    @Tool(description = "Verify Maven artifact SHA-1 checksum.")
    String artifactVerify(
            @ToolArg(description = "The artifact as groupId:artifactId:version") String gav,
            @ToolArg(description = "Expected SHA-1 hash") String sha1) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ToolboxCommandoImpl toolboxCommando = createToolboxCommando(outputStream)) {
            toolboxCommando.verify(ContextOverrides.CENTRAL, gav, sha1, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return outputStream.toString(StandardCharsets.UTF_8);
    }

    @Tool(description = "Calculate libyear metric for a Maven artifact to measure dependency freshness.")
    String artifactLibYear(@ToolArg(description = "The artifact as groupId:artifactId:version") String gav) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ToolboxCommandoImpl toolboxCommando = createToolboxCommando(outputStream)) {
            toolboxCommando.libYear(
                    "artifact",
                    ResolutionScope.RUNTIME,
                    ResolutionRoot.ofLoaded(new DefaultArtifact(gav)).build(),
                    true,
                    false,
                    ArtifactVersionMatcher.any(),
                    ArtifactVersionSelector.contextualSnapshotsAndPreviews(ArtifactVersionSelector.last()),
                    null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return outputStream.toString(StandardCharsets.UTF_8);
    }

    @Tool(description = "Get the classpath for a Maven artifact (list of all transitive dependencies).")
    String artifactClasspath(@ToolArg(description = "The artifact as groupId:artifactId:version") String gav) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ToolboxCommandoImpl toolboxCommando = createToolboxCommando(outputStream)) {
            toolboxCommando.classpath(
                    ResolutionScope.RUNTIME,
                    java.util.Collections.singleton(
                            ResolutionRoot.ofLoaded(new DefaultArtifact(gav)).build()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return outputStream.toString(StandardCharsets.UTF_8);
    }

    @Tool(description = "Compare classpaths of two Maven artifacts and show differences.")
    String artifactClasspathDiff(
            @ToolArg(description = "First artifact as groupId:artifactId:version") String gav1,
            @ToolArg(description = "Second artifact as groupId:artifactId:version") String gav2) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ToolboxCommandoImpl toolboxCommando = createToolboxCommando(outputStream)) {
            toolboxCommando.classpathDiff(
                    ResolutionScope.RUNTIME,
                    ResolutionRoot.ofLoaded(new DefaultArtifact(gav1)).build(),
                    ResolutionRoot.ofLoaded(new DefaultArtifact(gav2)).build(),
                    true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return outputStream.toString(StandardCharsets.UTF_8);
    }

    @Tool(description = "Detect classpath conflicts between two Maven artifacts.")
    String artifactClasspathConflict(
            @ToolArg(description = "First artifact as groupId:artifactId:version") String gav1,
            @ToolArg(description = "Second artifact as groupId:artifactId:version") String gav2) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ToolboxCommandoImpl toolboxCommando = createToolboxCommando(outputStream)) {
            toolboxCommando.classpathConflict(
                    ResolutionScope.RUNTIME,
                    ResolutionRoot.ofLoaded(new DefaultArtifact(gav1)).build(),
                    ResolutionRoot.ofLoaded(new DefaultArtifact(gav2)).build(),
                    ArtifactKeyFactory.ga(),
                    java.util.Collections.emptyMap());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return outputStream.toString(StandardCharsets.UTF_8);
    }

    @Tool(description = "Compare dependency trees of two Maven artifacts and show differences.")
    String artifactTreeDiff(
            @ToolArg(description = "First artifact as groupId:artifactId:version") String gav1,
            @ToolArg(description = "Second artifact as groupId:artifactId:version") String gav2) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ToolboxCommandoImpl toolboxCommando = createToolboxCommando(outputStream)) {
            toolboxCommando.treeDiff(
                    ResolutionScope.RUNTIME,
                    ResolutionRoot.ofLoaded(new DefaultArtifact(gav1)).build(),
                    ResolutionRoot.ofLoaded(new DefaultArtifact(gav2)).build(),
                    true,
                    DependencyMatcher.any());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return outputStream.toString(StandardCharsets.UTF_8);
    }

    @Tool(description = "List repositories that would be used to resolve a Maven artifact.")
    String artifactRepositories(@ToolArg(description = "The artifact as groupId:artifactId:version") String gav) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ToolboxCommandoImpl toolboxCommando = createToolboxCommando(outputStream)) {
            toolboxCommando.listRepositories(
                    ResolutionScope.RUNTIME,
                    "artifact",
                    ResolutionRoot.ofLoaded(new DefaultArtifact(gav)).build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return outputStream.toString(StandardCharsets.UTF_8);
    }
}
