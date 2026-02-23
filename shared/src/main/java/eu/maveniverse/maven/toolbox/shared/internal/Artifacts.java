/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import org.eclipse.aether.artifact.Artifact;

/**
 * Artifacts.
 */
public final class Artifacts {
    private Artifacts() {}

    public interface Sink extends eu.maveniverse.maven.toolbox.shared.Sink<Artifact> {}

    public interface Source extends eu.maveniverse.maven.toolbox.shared.Source<Artifact> {}
}
