/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.input;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StringSlurperTest {
    @Test
    void csv() throws IOException {
        Assertions.assertEquals(List.of("a", "b", "c"), StringSlurper.slurp("a,b,c"));
        Assertions.assertEquals(List.of("a", "b", "c"), StringSlurper.slurp("a|b|c"));
        Assertions.assertEquals(List.of("a", "b", "c"), StringSlurper.slurp("a;b;c"));
    }
}
