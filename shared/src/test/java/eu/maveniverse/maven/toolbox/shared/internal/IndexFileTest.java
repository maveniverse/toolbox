/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class IndexFileTest {
    @Test
    void flat(@TempDir Path target) throws Exception {
        Path content = Files.write(target.resolve("somecontent"), "somecontent".getBytes(StandardCharsets.UTF_8));
        Path indexFile;
        try (DirectorySink sink = DirectorySink.repository(target)) {
            indexFile = sink.getIndexFile();
            sink.accept(Arrays.asList(
                    new DefaultArtifact("g:a1:1").setFile(content.toFile()),
                    new DefaultArtifact("g:a2:1").setFile(content.toFile())));
        }

        Assertions.assertTrue(Files.isRegularFile(indexFile));
        List<String> lines = Files.readAllLines(indexFile);
        Assertions.assertSame(lines.size(), 2);
        Assertions.assertTrue(lines.contains("g:a1:jar:1 >> g/a1/1/a1-1.jar"));
        Assertions.assertTrue(lines.contains("g:a2:jar:1 >> g/a2/1/a2-1.jar"));

        try (DirectorySource source = DirectorySource.directory(target)) {
            List<Artifact> artifacts = source.get().collect(Collectors.toList());
            Assertions.assertEquals(2, artifacts.size());
        }
    }

    @Test
    void flatAppend(@TempDir Path target) throws IOException {
        Path content = Files.write(target.resolve("somecontent"), "somecontent".getBytes(StandardCharsets.UTF_8));
        Path indexFile;
        try (DirectorySink sink = DirectorySink.repository(target)) {
            indexFile = sink.getIndexFile();
            Assertions.assertFalse(Files.isRegularFile(indexFile));
            sink.accept(Arrays.asList(
                    new DefaultArtifact("g:a1:1").setFile(content.toFile()),
                    new DefaultArtifact("g:a2:1").setFile(content.toFile())));
        }
        Assertions.assertTrue(Files.isRegularFile(indexFile));
        try (DirectorySink sink = DirectorySink.repository(target)) {
            sink.accept(Arrays.asList(
                    new DefaultArtifact("g:a3:1").setFile(content.toFile()),
                    new DefaultArtifact("g:a4:1").setFile(content.toFile())));
        }
        Assertions.assertTrue(Files.isRegularFile(indexFile));

        List<String> lines = Files.readAllLines(indexFile);
        Assertions.assertSame(lines.size(), 4);
        Assertions.assertTrue(lines.contains("g:a1:jar:1 >> g/a1/1/a1-1.jar"));
        Assertions.assertTrue(lines.contains("g:a2:jar:1 >> g/a2/1/a2-1.jar"));
        Assertions.assertTrue(lines.contains("g:a3:jar:1 >> g/a3/1/a3-1.jar"));
        Assertions.assertTrue(lines.contains("g:a4:jar:1 >> g/a4/1/a4-1.jar"));
    }

    @Test
    void flatAppendRestore(@TempDir Path target) throws IOException {
        Path content = Files.write(target.resolve("somecontent"), "somecontent".getBytes(StandardCharsets.UTF_8));
        Path indexFile;
        try (DirectorySink sink = DirectorySink.repository(target)) {
            indexFile = sink.getIndexFile();
            Assertions.assertFalse(Files.isRegularFile(indexFile));
            sink.accept(Arrays.asList(
                    new DefaultArtifact("g:a1:1").setFile(content.toFile()),
                    new DefaultArtifact("g:a2:1").setFile(content.toFile())));
        }
        Assertions.assertTrue(Files.isRegularFile(indexFile));
        try (DirectorySink sink = DirectorySink.repository(target)) {
            sink.accept(Arrays.asList(
                    new DefaultArtifact("g:a3:1").setFile(content.toFile()),
                    new DefaultArtifact("g:a4:1").setFile(content.toFile())));
            sink.cleanup(new IOException("boo"));
        }
        Assertions.assertTrue(Files.isRegularFile(indexFile));

        List<String> lines = Files.readAllLines(indexFile);
        Assertions.assertSame(lines.size(), 2);
        Assertions.assertTrue(lines.contains("g:a1:jar:1 >> g/a1/1/a1-1.jar"));
        Assertions.assertTrue(lines.contains("g:a2:jar:1 >> g/a2/1/a2-1.jar"));
    }
}
