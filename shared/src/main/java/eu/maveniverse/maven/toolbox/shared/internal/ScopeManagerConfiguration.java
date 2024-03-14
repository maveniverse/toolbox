/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import eu.maveniverse.maven.toolbox.shared.DependencyScope;
import eu.maveniverse.maven.toolbox.shared.ResolutionScope;
import java.util.Collection;
import java.util.Optional;

/**
 * Internal scope manager.
 */
public interface ScopeManagerConfiguration {
    String getId();

    boolean isStrictDependencyScopes();

    boolean isStrictResolutionScopes();

    boolean isSystemScopeTransitive();

    boolean isBrokenRuntimeResolution();

    BuildScopeSource getBuildScopeSource();

    Optional<String> getSystemDependencyScopeLabel();

    Collection<DependencyScope> buildDependencyScopes(InternalScopeManager internalScopeManager);

    Collection<ResolutionScope> buildResolutionScopes(InternalScopeManager internalScopeManager);
}
