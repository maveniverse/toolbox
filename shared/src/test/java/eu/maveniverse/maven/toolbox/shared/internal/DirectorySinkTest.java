/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.maveniverse.maven.toolbox.shared.ArtifactNameMapper;
import eu.maveniverse.maven.toolbox.shared.output.NopOutput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class DirectorySinkTest {
    @Test
    void flat(@TempDir Path source, @TempDir Path target) throws IOException {
        try (DirectorySink sink = DirectorySink.flat(NopOutput.INSTANCE, target, ArtifactNameMapper.GACE(), false)) {
            Path a1 = source.resolve("a1");
            Path a2 = source.resolve("a2");
            Files.writeString(a1, "one", StandardCharsets.UTF_8);
            Files.writeString(a2, "two", StandardCharsets.UTF_8);
            sink.accept(Arrays.asList(
                    new DefaultArtifact("g:a1:1").setFile(a1.toFile()),
                    new DefaultArtifact("g:a2:1").setFile(a2.toFile())));

            Path a1target = target.resolve("g.a1.jar");
            Path a2target = target.resolve("g.a2.jar");
            assertTrue(Files.isRegularFile(a1target));
            assertEquals(Files.readString(a1target, StandardCharsets.UTF_8), "one");
            assertTrue(Files.isRegularFile(a2target));
            assertEquals(Files.readString(a2target, StandardCharsets.UTF_8), "two");
        }
    }

    @Test
    void repository(@TempDir Path source, @TempDir Path target) throws IOException {
        try (DirectorySink sink = DirectorySink.repository(NopOutput.INSTANCE, target, false)) {
            Path a1 = source.resolve("a1");
            Path a2 = source.resolve("a2");
            Files.writeString(a1, "one", StandardCharsets.UTF_8);
            Files.writeString(a2, "two", StandardCharsets.UTF_8);
            sink.accept(Arrays.asList(
                    new DefaultArtifact("g:a1:1").setFile(a1.toFile()),
                    new DefaultArtifact("g:a2:1").setFile(a2.toFile())));

            Path a1target = target.resolve("g/a1/1/a1-1.jar");
            Path a2target = target.resolve("g/a2/1/a2-1.jar");
            assertTrue(Files.isRegularFile(a1target));
            assertEquals(Files.readString(a1target, StandardCharsets.UTF_8), "one");
            assertTrue(Files.isRegularFile(a2target));
            assertEquals(Files.readString(a2target, StandardCharsets.UTF_8), "two");
        }
    }

    @Test
    void flatSameADifferentGAccepted(@TempDir Path source, @TempDir Path target) throws IOException {
        sameADifferentGAccepted(
                source, target, DirectorySink.flat(NopOutput.INSTANCE, target, ArtifactNameMapper.GACE(), false));
    }

    @Test
    void repositorySameADifferentGAccepted(@TempDir Path source, @TempDir Path target) throws IOException {
        sameADifferentGAccepted(source, target, DirectorySink.repository(NopOutput.INSTANCE, target, false));
    }

    private void sameADifferentGAccepted(Path source, Path target, DirectorySink sink) throws IOException {
        try (sink) {
            Path a1 = source.resolve("a1");
            Path a2 = source.resolve("a2");
            Files.writeString(a1, "one", StandardCharsets.UTF_8);
            Files.writeString(a2, "two", StandardCharsets.UTF_8);
            // we do not throw
            sink.accept(Arrays.asList(
                    new DefaultArtifact("g1:a1:1").setFile(a1.toFile()),
                    new DefaultArtifact("g2:a1:1").setFile(a2.toFile())));
        }
    }
}
