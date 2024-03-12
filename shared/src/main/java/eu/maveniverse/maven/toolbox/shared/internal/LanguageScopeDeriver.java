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
import eu.maveniverse.maven.toolbox.shared.Language;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.eclipse.aether.util.graph.transformer.ConflictResolver.ScopeContext;
import org.eclipse.aether.util.graph.transformer.ConflictResolver.ScopeDeriver;

/**
 * A scope deriver for use with {@link ConflictResolver} that supports the scopes from {@link Language}.
 * <p>
 * This class also "bridges" between {@link DependencyScope} and Resolver that uses plain string labels for scopes.
 *
 * @since 4.0.0
 */
public final class LanguageScopeDeriver extends ScopeDeriver {
    private final Language language;

    public LanguageScopeDeriver(Language language) {
        this.language = requireNonNull(language, "language");
    }

    @Override
    public void deriveScope(ScopeContext context) {
        context.setDerivedScope(getDerivedScope(context.getParentScope(), context.getChildScope()));
    }

    private String getDerivedScope(String parentScope, String childScope) {
        final AtomicReference<String> derivedScope = new AtomicReference<>("");

        // ask child scope
        // if present, invoke deriveFromParent w/ asked parent scope
        // if result present, set in derivedScope
        language.getDependencyScope(childScope)
                .flatMap(dependencyScope -> dependencyScope.deriveFromParent(
                        language.getDependencyScope(parentScope).orElse(null)))
                .ifPresent(scope -> derivedScope.set(scope.getId()));

        return derivedScope.get();
    }
}
