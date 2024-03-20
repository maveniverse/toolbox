/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.cli;

import eu.maveniverse.maven.mima.context.Context;
import picocli.CommandLine;

@CommandLine.Command(name = "test", description = "Test console")
public class Test extends CommandSupport {
    @Override
    public boolean doCall(Context context) {
        output.verbose("Verbose: {}", "foo", new RuntimeException("runtime"));
        output.normal("Normal: {}", "foo", new RuntimeException("runtime"));
        output.warn("Warning: {}", "foo", new RuntimeException("runtime"));
        output.error("Error: {}", "foo", new RuntimeException("runtime"));
        return true;
    }
}
