/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared.internal;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.toolbox.shared.BuildScope;
import eu.maveniverse.maven.toolbox.shared.DependencyScope;
import eu.maveniverse.maven.toolbox.shared.Language;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.collection.DependencyGraphTransformationContext;
import org.eclipse.aether.collection.DependencyGraphTransformer;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;

/**
 * A dependency graph transformer that refines the request context for nodes that belong to the "project" context by
 * appending the buildpath type to which the node belongs. For instance, a compile-time project dependency will be
 * assigned the request context "project/compile".
 *
 * <p>
 * This class also "bridges" between {@link DependencyScope} and Resolver that uses plain string labels for scopes.
 *
 * @see DependencyNode#getRequestContext()
 */
public final class LanguageDependencyContextRefiner implements DependencyGraphTransformer {
    private final Language language;

    public LanguageDependencyContextRefiner(Language language) {
        this.language = requireNonNull(language, "language");
    }

    @Override
    public DependencyNode transformGraph(DependencyNode node, DependencyGraphTransformationContext context)
            throws RepositoryException {
        requireNonNull(node, "node cannot be null");
        requireNonNull(context, "context cannot be null");
        String ctx = node.getRequestContext();

        if ("project".equals(ctx)) {
            String scope = getBuildpathScope(node);
            if (scope != null) {
                ctx += '/' + scope;
                node.setRequestContext(ctx);
            }
        }

        for (DependencyNode child : node.getChildren()) {
            transformGraph(child, context);
        }

        return node;
    }

    private String getBuildpathScope(DependencyNode node) {
        Dependency dependency = node.getDependency();
        if (dependency == null) {
            return null;
        }

        return language.getDependencyScope(dependency.getScope())
                .flatMap(s -> s.getMainProjectBuildScope().map(BuildScope::getId))
                .orElse(null);
    }
}
