/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkiverse.mcp.server.test.McpAssured;
import io.quarkiverse.mcp.server.test.McpAssured.McpStreamableTestClient;
import java.util.Map;

// @QuarkusTest
public class ToolboxToolsAnswerTest {

    // @Test
    public void testAnswer() {
        McpStreamableTestClient client = McpAssured.newConnectedStreamableClient();

        client.when()
                .toolsCall("theAnswer", Map.of("lang", "Java"), r -> {
                    assertEquals(
                            "Spaces are better for indentation.",
                            r.content().get(0).asText().text());
                })
                .toolsCall("theAnswer", Map.of("lang", "python"), r -> {
                    assertEquals(
                            "Tabs are better for indentation.",
                            r.content().get(0).asText().text());
                })
                .thenAssertResults();
    }
}
