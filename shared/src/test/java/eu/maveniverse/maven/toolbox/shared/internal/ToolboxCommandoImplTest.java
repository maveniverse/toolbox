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
import eu.maveniverse.maven.toolbox.shared.DeployingSink;
import eu.maveniverse.maven.toolbox.shared.DirectorySink;
import eu.maveniverse.maven.toolbox.shared.InstallingSink;
import eu.maveniverse.maven.toolbox.shared.NullOutput;
import eu.maveniverse.maven.toolbox.shared.PurgingSink;
import java.io.IOException;
import java.nio.file.Path;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ToolboxCommandoImplTest {
    @Test
    void artifactSinkSpec(@TempDir Path tempDir) throws IOException {
        Runtime runtime = Runtimes.INSTANCE.getRuntime();
        try (Context context = runtime.create(ContextOverrides.create().build())) {
            ToolboxCommandoImpl commando = new ToolboxCommandoImpl(runtime, context);
            ArtifactSink sink;

            sink = commando.artifactSink(new NullOutput(), tempDir.toString());
            assertInstanceOf(DirectorySink.class, sink);
            assertEquals(tempDir, ((DirectorySink) sink).getDirectory());

            sink = commando.artifactSink(new NullOutput(), "flat:" + tempDir);
            assertInstanceOf(DirectorySink.class, sink);
            assertEquals(tempDir, ((DirectorySink) sink).getDirectory());

            sink = commando.artifactSink(new NullOutput(), "flat:" + tempDir + ",AVCE()");
            assertInstanceOf(DirectorySink.class, sink);
            assertEquals(tempDir, ((DirectorySink) sink).getDirectory());

            sink = commando.artifactSink(new NullOutput(), "repository:" + tempDir);
            assertInstanceOf(DirectorySink.class, sink);
            assertEquals(tempDir, ((DirectorySink) sink).getDirectory());

            sink = commando.artifactSink(new NullOutput(), "deploy:id::https://somewhere.com/");
            assertInstanceOf(DeployingSink.class, sink);
            assertEquals(
                    ((DeployingSink) sink).getRemoteRepository(),
                    new RemoteRepository.Builder("id", "default", "https://somewhere.com/").build());

            sink = commando.artifactSink(new NullOutput(), "install:" + tempDir);
            assertInstanceOf(InstallingSink.class, sink);
            assertEquals(
                    tempDir,
                    ((InstallingSink) sink).getLocalRepository().getBasedir().toPath());

            sink = commando.artifactSink(new NullOutput(), "purge:" + tempDir);
            assertInstanceOf(PurgingSink.class, sink);
            assertEquals(
                    tempDir,
                    ((PurgingSink) sink).getLocalRepository().getBasedir().toPath());
        }
    }
}
