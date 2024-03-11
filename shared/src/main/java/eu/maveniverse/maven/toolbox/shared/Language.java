/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

import java.util.Collection;
import java.util.Optional;

public interface Language {
    String getId();

    String getDescription();

    Optional<DependencyScope> getDependencyScope(String id);

    Collection<? extends DependencyScope> getDependencyScopeUniverse();

    Optional<ResolutionScope> getResolutionScope(String id);

    Collection<? extends ResolutionScope> getResolutionScopeUniverse();
}
