/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
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
