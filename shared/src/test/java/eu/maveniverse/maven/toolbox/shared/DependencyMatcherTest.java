/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.VersionScheme;
import org.junit.jupiter.api.Test;

public class DependencyMatcherTest {
    private final VersionScheme versionScheme = new GenericVersionScheme();
    private final Dependency dependency1 = new Dependency(new DefaultArtifact("g:a:1.0"), "compile", false);
    private final Dependency dependency2 =
            new Dependency(new DefaultArtifact("g:a:jar:classifier:1.0-20240322.113300-2"), "runtime", true);

    @Test
    void any() {
        assertTrue(DependencyMatcher.any().test(dependency1));
        assertTrue(DependencyMatcher.any().test(dependency2));
    }

    @Test
    void composedAnd() {
        assertTrue(DependencyMatcher.and(
                        DependencyMatcher.artifact(ArtifactMatcher.artifact("g:*:*")),
                        DependencyMatcher.artifact(ArtifactMatcher.artifact("*:a:*")),
                        DependencyMatcher.artifact(ArtifactMatcher.artifact("*:*:1*")))
                .test(dependency1));
    }

    @Test
    void parse() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("groupId", "g2");
        DependencyMatcher parsed;

        parsed = DependencyMatcher.build(versionScheme, properties, "any()");
        assertTrue(parsed.test(dependency1));
        assertTrue(parsed.test(dependency2));

        parsed = DependencyMatcher.build(versionScheme, properties, "scopeIncluded(compile, runtime)");
        assertTrue(parsed.test(dependency1));
        assertTrue(parsed.test(dependency2));

        parsed = DependencyMatcher.build(versionScheme, properties, "scopeIncluded(compile)");
        assertTrue(parsed.test(dependency1));
        assertFalse(parsed.test(dependency2));

        parsed = DependencyMatcher.build(versionScheme, properties, "scopeExcluded(runtime)");
        assertTrue(parsed.test(dependency1));
        assertFalse(parsed.test(dependency2));

        parsed =
                DependencyMatcher.build(versionScheme, properties, "or(scopeIncluded(compile),scopeIncluded(runtime))");
        assertTrue(parsed.test(dependency1));
        assertTrue(parsed.test(dependency2));

        parsed = DependencyMatcher.build(versionScheme, properties, "optional(true)");
        assertFalse(parsed.test(dependency1));
        assertTrue(parsed.test(dependency2));

        parsed = DependencyMatcher.build(versionScheme, properties, "optional(false)");
        assertTrue(parsed.test(dependency1));
        assertFalse(parsed.test(dependency2));

        parsed = DependencyMatcher.build(versionScheme, properties, "artifact(*)");
        assertTrue(parsed.test(dependency1));
        assertTrue(parsed.test(dependency2));

        parsed = DependencyMatcher.build(versionScheme, properties, "artifact(*:*:classifier:*:*)");
        assertFalse(parsed.test(dependency1));
        assertTrue(parsed.test(dependency2));
    }
}
