/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import org.eclipse.aether.graph.Dependency;

/**
 * Dependencies.
 */
public final class Dependencies {
    private Dependencies() {}

    public interface Sink extends eu.maveniverse.maven.toolbox.shared.Sink<Dependency> {}

    public interface Source extends eu.maveniverse.maven.toolbox.shared.Source<Dependency> {}
}
