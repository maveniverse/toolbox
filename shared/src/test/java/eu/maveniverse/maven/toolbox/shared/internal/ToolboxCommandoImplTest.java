/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.Runtime;
import eu.maveniverse.maven.mima.context.Runtimes;
import eu.maveniverse.maven.toolbox.shared.output.NopOutput;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class ToolboxCommandoImplTest {

    @Test
    void normalizeSearchExpression() {
        // documented forms that Central search rejected (issue #450)
        assertEquals(
                "g:io.cucumber AND a:cucumber-core",
                ToolboxCommandoImpl.normalizeSearchExpression("g:io.cucumber a:cucumber-core"));
        assertEquals(
                "g:io.cucumber AND a:cucumber-core",
                ToolboxCommandoImpl.normalizeSearchExpression("io.cucumber:cucumber-core"));
        // forms that already worked stay as they are
        assertEquals("cucumber-core", ToolboxCommandoImpl.normalizeSearchExpression(" cucumber-core "));
        assertEquals("g:io.cucumber", ToolboxCommandoImpl.normalizeSearchExpression("g:io.cucumber"));
        assertEquals("junit:junit:4.13.2", ToolboxCommandoImpl.normalizeSearchExpression("junit:junit:4.13.2"));
        assertEquals(
                "g:io.cucumber OR a:cucumber-core",
                ToolboxCommandoImpl.normalizeSearchExpression("g:io.cucumber OR a:cucumber-core"));
        assertEquals("text:\"foo bar\"", ToolboxCommandoImpl.normalizeSearchExpression("text:\"foo bar\""));
    }

    @Disabled("This test does not test anything")
    @Test
    void search() throws IOException {
        Runtime runtime = Runtimes.INSTANCE.getRuntime();
        try (Context context = runtime.create(ContextOverrides.create()
                .withBasedirOverride(Path.of("target").toAbsolutePath())
                .build())) {
            ToolboxCommandoImpl tc = new ToolboxCommandoImpl(NopOutput.INSTANCE, context);

            tc.search(ContextOverrides.CENTRAL, "junit:junit:4.13.2");
        }
    }
}
