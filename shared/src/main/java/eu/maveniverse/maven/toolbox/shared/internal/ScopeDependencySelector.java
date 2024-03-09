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

import java.util.*;
import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.Dependency;

/**
 * A dependency selector that filters transitive dependencies based on their scope. It is configurable from which level
 * applies, as it depend on "as project" and "as dependency" use cases.
 * <p>
 * <em>Note:</em> This filter does not assume any relationships between the scopes.
 * In particular, the filter is not aware of scopes that logically include other scopes.
 *
 * @see Dependency#getScope()
 */
public final class ScopeDependencySelector implements DependencySelector {
    public static ScopeDependencySelector fromRoot(Collection<String> included, Collection<String> excluded) {
        return from(1, included, excluded);
    }

    public static ScopeDependencySelector fromDirect(Collection<String> included, Collection<String> excluded) {
        return from(2, included, excluded);
    }

    public static ScopeDependencySelector from(
            int applyFrom, Collection<String> included, Collection<String> excluded) {
        if (applyFrom < 1) {
            throw new IllegalArgumentException("applyFrom must be non-zero and positive");
        }
        return new ScopeDependencySelector(0, applyFrom, included, excluded);
    }

    private final int depth;

    private final int applyFrom;

    private final Collection<String> included;

    private final Collection<String> excluded;

    private ScopeDependencySelector(
            int depth, int applyFrom, Collection<String> included, Collection<String> excluded) {
        this.depth = depth;
        this.applyFrom = applyFrom;
        this.included = included;
        this.excluded = excluded;
    }

    @Override
    public boolean selectDependency(Dependency dependency) {
        requireNonNull(dependency, "dependency cannot be null");
        if (depth < applyFrom) {
            return true;
        }

        String scope = dependency.getScope();
        return (included == null || included.contains(scope)) && (excluded == null || !excluded.contains(scope));
    }

    @Override
    public DependencySelector deriveChildSelector(DependencyCollectionContext context) {
        requireNonNull(context, "context cannot be null");
        return new ScopeDependencySelector(depth + 1, applyFrom, included, excluded);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (null == obj || !getClass().equals(obj.getClass())) {
            return false;
        }

        ScopeDependencySelector that = (ScopeDependencySelector) obj;
        return depth == that.depth
                && applyFrom == that.applyFrom
                && Objects.equals(included, that.included)
                && Objects.equals(excluded, that.excluded);
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = hash * 31 + depth;
        hash = hash * 31 + applyFrom;
        hash = hash * 31 + (included != null ? included.hashCode() : 0);
        hash = hash * 31 + (excluded != null ? excluded.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return String.format(
                "%s(included: %s, excluded: %s, applied: %s)",
                getClass().getSimpleName(), included, excluded, depth >= applyFrom);
    }
}
