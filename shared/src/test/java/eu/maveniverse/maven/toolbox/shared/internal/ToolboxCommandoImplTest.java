/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.Runtime;
import eu.maveniverse.maven.mima.context.Runtimes;
import eu.maveniverse.maven.toolbox.shared.NullOutput;
import java.io.IOException;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

public class ToolboxCommandoImplTest {
    @Test
    void search() throws IOException {
        Runtime runtime = Runtimes.INSTANCE.getRuntime();
        try (Context context = runtime.create(ContextOverrides.create()
                .withBasedirOverride(Paths.get("target").toAbsolutePath())
                .build())) {
            ToolboxCommandoImpl tc = new ToolboxCommandoImpl(context);

            tc.search(ContextOverrides.CENTRAL, "junit:junit:4.13.2", new NullOutput());
        }
    }
}
