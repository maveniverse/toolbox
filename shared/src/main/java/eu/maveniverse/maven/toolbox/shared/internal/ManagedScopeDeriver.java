/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.toolbox.shared.DependencyScope;
import eu.maveniverse.maven.toolbox.shared.ScopeManager;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.eclipse.aether.util.graph.transformer.ConflictResolver.ScopeContext;
import org.eclipse.aether.util.graph.transformer.ConflictResolver.ScopeDeriver;

/**
 * A scope deriver for use with {@link ConflictResolver} that supports the scopes from {@link ScopeManager}. It basically
 * chooses "narrowest" scope, based on parent and child scopes.
 * <p>
 * This class also "bridges" between {@link DependencyScope} and Resolver that uses plain string labels for scopes.
 *
 * @since 4.0.0
 */
public final class ManagedScopeDeriver extends ScopeDeriver {
    private final ScopeManager scopeManager;
    private final DependencyScope systemScope;

    public ManagedScopeDeriver(ScopeManager scopeManager) {
        this.scopeManager = requireNonNull(scopeManager, "scopeManager");
        this.systemScope = scopeManager.getSystemScope().orElse(null);
    }

    @Override
    public void deriveScope(ScopeContext context) {
        context.setDerivedScope(getDerivedScope(context.getParentScope(), context.getChildScope()));
    }

    /**
     * Visible for testing. It chooses "narrowest" scope out of parent or child, unless child is system scope.
     */
    public String getDerivedScope(String parentScope, String childScope) {
        // ask parent scope (nullable)
        DependencyScope parent = parentScope != null
                ? scopeManager.getDependencyScope(parentScope).orElse(null)
                : null;
        // ask child scope (non-nullable, but may be unknown scope to manager)
        DependencyScope child = scopeManager.getDependencyScope(childScope).orElse(null);

        // if system scope exists and child is system scope: system
        if (systemScope != null && systemScope == child) {
            return systemScope.getId();
        }
        // if no parent (i.e. is root): child scope as-is
        if (parent == null) {
            return child != null ? child.getId() : "";
        }
        if (child == null) {
            return parent.getId();
        }
        // otherwise the narrowest out of parent or child
        if (parent.width() < child.width()) {
            return parent.getId();
        } else {
            return child.getId();
        }
    }
}
