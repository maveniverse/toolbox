/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.Runtime;
import eu.maveniverse.maven.mima.context.Runtimes;
import eu.maveniverse.maven.toolbox.shared.ArtifactSink;
import java.nio.file.Paths;
import java.util.HashMap;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.VersionScheme;
import org.junit.jupiter.api.Test;

public class ArtifactSinksTest {
    @Test
    void parse() {
        Runtime runtime = Runtimes.INSTANCE.getRuntime();
        try (Context context = runtime.create(ContextOverrides.create()
                .withBasedirOverride(Paths.get("target").toAbsolutePath())
                .build())) {
            ToolboxCommandoImpl tc = new ToolboxCommandoImpl(runtime, context);
            VersionScheme versionScheme = new GenericVersionScheme();

            HashMap<String, Object> properties = new HashMap<>();
            properties.put("groupId", "org.some.group");
            ArtifactSink artifactSink;

            artifactSink = ArtifactSinks.build(versionScheme, properties, tc, "null()");
            assertInstanceOf(ArtifactSinks.NullArtifactSink.class, artifactSink);

            artifactSink = ArtifactSinks.build(versionScheme, properties, tc, "matching(any(),null())");
            assertInstanceOf(ArtifactSinks.MatchingArtifactSink.class, artifactSink);

            artifactSink = ArtifactSinks.build(
                    versionScheme, properties, tc, "mapping(baseVersion(), matching(any(),null()))");
            assertInstanceOf(ArtifactSinks.MappingArtifactSink.class, artifactSink);

            artifactSink = ArtifactSinks.build(versionScheme, properties, tc, "tee(counting(), sizing())");
            assertInstanceOf(ArtifactSinks.TeeArtifactSink.class, artifactSink);

            artifactSink = ArtifactSinks.build(versionScheme, properties, tc, "install()");
            assertInstanceOf(InstallingSink.class, artifactSink);
            assertEquals(
                    ((InstallingSink) artifactSink).getLocalRepository().getBasedir(),
                    context.repositorySystemSession().getLocalRepository().getBasedir());

            artifactSink = ArtifactSinks.build(versionScheme, properties, tc, "install(some/path)");
            assertInstanceOf(InstallingSink.class, artifactSink);
            assertEquals(
                    ((InstallingSink) artifactSink).getLocalRepository().getBasedir(),
                    context.basedir().resolve("some/path").toFile());

            artifactSink = ArtifactSinks.build(versionScheme, properties, tc, "deploy(test::https://somewhere.com)");
            assertInstanceOf(DeployingSink.class, artifactSink);
            assertEquals(
                    ((DeployingSink) artifactSink).getRemoteRepository(),
                    new RemoteRepository.Builder("test", "default", "https://somewhere.com").build());

            artifactSink = ArtifactSinks.build(versionScheme, properties, tc, "unpack(some/path)");
            assertInstanceOf(UnpackSink.class, artifactSink);
            assertEquals(
                    ((UnpackSink) artifactSink).getDirectory(),
                    context.basedir().resolve("some/path"));

            artifactSink = ArtifactSinks.build(versionScheme, properties, tc, "unpack(some/path,ACE())");
            assertInstanceOf(UnpackSink.class, artifactSink);
            assertEquals(
                    ((UnpackSink) artifactSink).getDirectory(),
                    context.basedir().resolve("some/path"));

            artifactSink = ArtifactSinks.build(versionScheme, properties, tc, "repository(some/path)");
            assertInstanceOf(DirectorySink.class, artifactSink);
            assertEquals(
                    ((DirectorySink) artifactSink).getDirectory(),
                    context.basedir().resolve("some/path"));

            artifactSink = ArtifactSinks.build(versionScheme, properties, tc, "flat(some/path)");
            assertInstanceOf(DirectorySink.class, artifactSink);
            assertEquals(
                    ((DirectorySink) artifactSink).getDirectory(),
                    context.basedir().resolve("some/path"));

            artifactSink = ArtifactSinks.build(versionScheme, properties, tc, "flat(some/path,GACVE())");
            assertInstanceOf(DirectorySink.class, artifactSink);
            assertEquals(
                    ((DirectorySink) artifactSink).getDirectory(),
                    context.basedir().resolve("some/path"));
        }
    }
}
