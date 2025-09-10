/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.maveniverse.maven.toolbox.shared.output.NopOutput;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

public class ToolboxGraphImplTest {
    @Test
    void commonPrefix() {
        ToolboxGraphImpl graph = new ToolboxGraphImpl(NopOutput.INSTANCE);

        assertEquals("org.apache.maven", graph.commonPrefix(Collections.singleton("org.apache.maven")));
        assertEquals(
                "org.apache.maven", graph.commonPrefix(Arrays.asList("org.apache.maven", "org.apache.maven.plugins")));
        assertEquals(
                "", graph.commonPrefix(Arrays.asList("org.apache.maven", "org.apache.maven.plugins", "commons-cli")));
    }

    @Test
    void shortPrefix() {
        ToolboxGraphImpl graph = new ToolboxGraphImpl(NopOutput.INSTANCE);

        assertEquals("o.a.m", graph.shortPrefix("org.apache.maven"));
        assertEquals("o.c", graph.shortPrefix("org.cstamas"));
        assertEquals("commons-cli", graph.shortPrefix("commons-cli"));
    }

    @Test
    void commonAndShortPrefix() {
        ToolboxGraphImpl graph = new ToolboxGraphImpl(NopOutput.INSTANCE);

        assertEquals(
                "org.apache.maven", graph.commonPrefix(Arrays.asList("org.apache.maven", "org.apache.maven.plugins")));
        assertEquals(
                "", graph.commonPrefix(Arrays.asList("org.apache.maven", "org.apache.maven.plugins", "commons-cli")));
    }
}
