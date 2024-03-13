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
import java.util.*;
import java.util.stream.Collectors;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.eclipse.aether.util.graph.transformer.ConflictResolver.ConflictContext;
import org.eclipse.aether.util.graph.transformer.ConflictResolver.ConflictItem;
import org.eclipse.aether.util.graph.transformer.ConflictResolver.ScopeSelector;

/**
 * A scope selector for use with {@link ConflictResolver} that supports the scopes from {@link Language}.
 * In general, this selector picks the widest scope present among conflicting dependencies where e.g. "compile" is
 * wider than "runtime" which is wider than "test". If however a direct dependency is involved, its scope is selected.
 * <p>
 * This class also "bridges" between {@link DependencyScope} and Resolver that uses plain string labels for scopes.
 */
public final class LanguageScopeSelector extends ScopeSelector {
    private final DependencyScope systemScope;
    private final List<DependencyScope> dependencyScopesByWidthDescending;

    public LanguageScopeSelector(Language language) {
        requireNonNull(language, "language");
        this.systemScope = language.getSystemScope().orElse(null);
        this.dependencyScopesByWidthDescending =
                Collections.unmodifiableList(language.getDependencyScopeUniverse().stream()
                        .sorted(Comparator.comparing(DependencyScope::width).reversed())
                        .collect(Collectors.toList()));
    }

    @Override
    public void selectScope(ConflictContext context) {
        String scope = context.getWinner().getDependency().getScope();
        if (systemScope == null || !systemScope.getId().equals(scope)) {
            scope = chooseEffectiveScope(context.getItems());
        }
        context.setScope(scope);
    }

    private String chooseEffectiveScope(Collection<ConflictItem> items) {
        Set<String> scopes = new HashSet<>();
        for (ConflictItem item : items) {
            if (item.getDepth() <= 1) {
                return item.getDependency().getScope();
            }
            scopes.addAll(item.getScopes());
        }
        return chooseEffectiveScope(scopes);
    }

    /**
     * Visible for testing. It chooses "widest" scope out of provided ones, unless system scope is present, in which
     * case system scope is selected.
     */
    public String chooseEffectiveScope(Set<String> scopes) {
        if (scopes.size() > 1 && systemScope != null) {
            scopes.remove(systemScope.getId());
        }

        String effectiveScope = "";

        if (scopes.size() == 1) {
            effectiveScope = scopes.iterator().next();
        } else {
            for (DependencyScope dependencyScope : dependencyScopesByWidthDescending) {
                if (scopes.contains(dependencyScope.getId())) {
                    return dependencyScope.getId();
                }
            }
        }

        return effectiveScope;
    }
}
