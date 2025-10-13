/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.Runtime;
import eu.maveniverse.maven.mima.context.Runtimes;
import eu.maveniverse.maven.toolbox.shared.Sink;
import eu.maveniverse.maven.toolbox.shared.output.NopOutput;
import java.nio.file.Path;
import java.util.HashMap;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.jupiter.api.Test;

public class ArtifactSinksTest {
    @Test
    void parse() {
        Runtime runtime = Runtimes.INSTANCE.getRuntime();
        try (Context context = runtime.create(ContextOverrides.create()
                .withBasedirOverride(Path.of("target").toAbsolutePath())
                .build())) {
            ToolboxCommandoImpl tc = new ToolboxCommandoImpl(NopOutput.INSTANCE, context);

            HashMap<String, Object> properties = new HashMap<>();
            properties.put("groupId", "org.some.group");
            Sink<Artifact> artifactSink;

            artifactSink = ArtifactSinks.build(properties, tc, false, "null()");
            assertInstanceOf(ArtifactSinks.NullArtifactSink.class, artifactSink);

            artifactSink = ArtifactSinks.build(properties, tc, false, "matching(any(),null())");
            assertInstanceOf(ArtifactSinks.MatchingArtifactSink.class, artifactSink);

            artifactSink = ArtifactSinks.build(properties, tc, false, "mapping(baseVersion(), matching(any(),null()))");
            assertInstanceOf(ArtifactSinks.MappingArtifactSink.class, artifactSink);

            artifactSink = ArtifactSinks.build(properties, tc, false, "tee(counting(), sizing())");
            assertInstanceOf(ArtifactSinks.TeeArtifactSink.class, artifactSink);

            artifactSink = ArtifactSinks.build(properties, tc, false, "install()");
            assertInstanceOf(InstallingSink.class, artifactSink);
            assertEquals(
                    ((InstallingSink) artifactSink).getLocalRepository().getBasedir(),
                    context.repositorySystemSession().getLocalRepository().getBasedir());

            artifactSink = ArtifactSinks.build(properties, tc, false, "install(some/path)");
            assertInstanceOf(InstallingSink.class, artifactSink);
            assertEquals(
                    ((InstallingSink) artifactSink).getLocalRepository().getBasedir(),
                    context.basedir().resolve("some/path").toFile());

            artifactSink = ArtifactSinks.build(properties, tc, false, "deploy(test::https://somewhere.com)");
            assertInstanceOf(DeployingSink.class, artifactSink);
            assertEquals(
                    ((DeployingSink) artifactSink).getRemoteRepository(),
                    new RemoteRepository.Builder("test", "default", "https://somewhere.com").build());

            artifactSink = ArtifactSinks.build(properties, tc, false, "unpack(some/path)");
            assertInstanceOf(UnpackSink.class, artifactSink);
            assertEquals(
                    ((UnpackSink) artifactSink).getDirectory(),
                    context.basedir().resolve("some/path"));

            artifactSink = ArtifactSinks.build(properties, tc, false, "unpack(some/path,ACE())");
            assertInstanceOf(UnpackSink.class, artifactSink);
            assertEquals(
                    ((UnpackSink) artifactSink).getDirectory(),
                    context.basedir().resolve("some/path"));

            artifactSink = ArtifactSinks.build(properties, tc, false, "repository(some/path)");
            assertInstanceOf(DirectorySink.class, artifactSink);
            assertEquals(
                    ((DirectorySink) artifactSink).getDirectory(),
                    context.basedir().resolve("some/path"));

            artifactSink = ArtifactSinks.build(properties, tc, false, "flat(some/path)");
            assertInstanceOf(DirectorySink.class, artifactSink);
            assertEquals(
                    ((DirectorySink) artifactSink).getDirectory(),
                    context.basedir().resolve("some/path"));

            artifactSink = ArtifactSinks.build(properties, tc, false, "flat(some/path,GACVE())");
            assertInstanceOf(DirectorySink.class, artifactSink);
            assertEquals(
                    ((DirectorySink) artifactSink).getDirectory(),
                    context.basedir().resolve("some/path"));

            ArtifactSinks.StatArtifactSink stat;
            artifactSink = ArtifactSinks.build(properties, tc, false, "stat()");
            assertInstanceOf(ArtifactSinks.StatArtifactSink.class, artifactSink);
            stat = (ArtifactSinks.StatArtifactSink) artifactSink;
            assertTrue(stat.isList());
            assertTrue(stat.isDetails());

            artifactSink = ArtifactSinks.build(properties, tc, false, "stat(false)");
            assertInstanceOf(ArtifactSinks.StatArtifactSink.class, artifactSink);
            stat = (ArtifactSinks.StatArtifactSink) artifactSink;
            assertTrue(stat.isList());
            assertFalse(stat.isDetails());

            artifactSink = ArtifactSinks.build(properties, tc, false, "stat(false,false)");
            assertInstanceOf(ArtifactSinks.StatArtifactSink.class, artifactSink);
            stat = (ArtifactSinks.StatArtifactSink) artifactSink;
            assertFalse(stat.isList());
            assertFalse(stat.isDetails());
        }
    }
}
