/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static org.junit.jupiter.api.Assertions.*;

import eu.maveniverse.maven.toolbox.shared.NullOutput;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class IndexFileSinkTest {
    @Test
    void flat(@TempDir Path target) throws IOException {
        Path indexFile = target.resolve("index.txt");
        try (IndexFileSink sink = IndexFileSink.flat(new NullOutput(), indexFile, false)) {
            sink.accept(Arrays.asList(new DefaultArtifact("g:a1:1"), new DefaultArtifact("g:a2:1")));
        }

        assertTrue(Files.isRegularFile(indexFile));
        List<String> lines = Files.readAllLines(indexFile);
        assertSame(lines.size(), 2);
        assertTrue(lines.contains("g:a1:jar:1"));
        assertTrue(lines.contains("g:a2:jar:1"));
    }

    @Test
    void flatAppend(@TempDir Path target) throws IOException {
        Path indexFile = target.resolve("index.txt");
        assertFalse(Files.isRegularFile(indexFile));
        try (IndexFileSink sink = IndexFileSink.flat(new NullOutput(), indexFile, false)) {
            sink.accept(Arrays.asList(new DefaultArtifact("g:a1:1"), new DefaultArtifact("g:a2:1")));
        }
        assertTrue(Files.isRegularFile(indexFile));
        try (IndexFileSink sink = IndexFileSink.flat(new NullOutput(), indexFile, true)) {
            sink.accept(Arrays.asList(new DefaultArtifact("g:a3:1"), new DefaultArtifact("g:a4:1")));
        }
        assertTrue(Files.isRegularFile(indexFile));

        List<String> lines = Files.readAllLines(indexFile);
        assertSame(lines.size(), 4);
        assertTrue(lines.contains("g:a1:jar:1"));
        assertTrue(lines.contains("g:a2:jar:1"));
        assertTrue(lines.contains("g:a3:jar:1"));
        assertTrue(lines.contains("g:a4:jar:1"));
    }

    @Test
    void flatAppendRestore(@TempDir Path target) throws IOException {
        Path indexFile = target.resolve("index.txt");
        assertFalse(Files.isRegularFile(indexFile));
        try (IndexFileSink sink = IndexFileSink.flat(new NullOutput(), indexFile, false)) {
            sink.accept(Arrays.asList(new DefaultArtifact("g:a1:1"), new DefaultArtifact("g:a2:1")));
        }
        assertTrue(Files.isRegularFile(indexFile));
        try (IndexFileSink sink = IndexFileSink.flat(new NullOutput(), indexFile, true)) {
            sink.accept(Arrays.asList(new DefaultArtifact("g:a3:1"), new DefaultArtifact("g:a4:1")));
            sink.cleanup(new IOException("boo"));
        }
        assertTrue(Files.isRegularFile(indexFile));

        List<String> lines = Files.readAllLines(indexFile);
        assertSame(lines.size(), 2);
        assertTrue(lines.contains("g:a1:jar:1"));
        assertTrue(lines.contains("g:a2:jar:1"));
    }
}
