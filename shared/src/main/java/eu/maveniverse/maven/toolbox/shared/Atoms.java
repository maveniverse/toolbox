/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * The "atoms" of the Toolbox that are used to assemble language specific dependency scopes and resolution scopes.
 * <p>
 * Essentially, we deal with 3-dimensional space: project scope, resolution scope, resolution mode. The lagnauges where languages
 * place their scopes.
 */
public final class Atoms {
    /**
     * Base atom class: is basically "label" or "id" w/ equals/hash implemented using ONLY that (as the rest are
     * language specific properties, but within one language only one label may exist).
     */
    public static class Atom {
        protected final String id;

        protected Atom(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Atom atom = (Atom) o;
            return Objects.equals(id, atom.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "(" + id + ")";
        }
    }

    /**
     * Dependency scope: all the variations for all resolution scopes.
     */
    public static final class DependencyScope extends Atom {
        //
        //                   compile | runtime
        // none              no      | no
        // both              yes     | yes
        // onlyRuntime       no      | yes
        // onlyCompile       yes     | no
        public static final DependencyScope NONE = new DependencyScope("none");
        public static final DependencyScope BOTH = new DependencyScope("both");
        public static final DependencyScope ONLY_RUNTIME = new DependencyScope("onlyRuntime");
        public static final DependencyScope ONLY_COMPILE = new DependencyScope("onlyCompile");

        public static final Set<DependencyScope> ALL =
                Collections.unmodifiableSet(new HashSet<>(Arrays.asList(NONE, BOTH, ONLY_RUNTIME, ONLY_COMPILE)));

        public DependencyScope(String id) {
            super(id);
        }
    }

    /**
     * Project scope: like "main", "test", etc...
     */
    public static final class ProjectScope extends Atom {
        public static final ProjectScope NONE = new ProjectScope("none");
        public static final ProjectScope MAIN = new ProjectScope("main");
        public static final ProjectScope TEST = new ProjectScope("test");
        public static final Set<ProjectScope> ALL =
                Collections.unmodifiableSet(new HashSet<>(Arrays.asList(NONE, MAIN, TEST)));

        // TODO: this could be even extended? IT, etc

        public ProjectScope(String id) {
            super(id);
        }
    }

    /**
     * Resolution mode: eliminate or just remove.
     */
    public enum ResolutionMode {
        /**
         * Mode where non-wanted scopes are eliminated. In other words, this mode ensures that if a dependency was
         * removed, as it was in unwanted scope, it will guarantee that no such dependency will appear anywhere else in
         * the resulting graph.
         */
        ELIMINATE,

        /**
         * Mode where non-wanted scopes are removed only. In other words, they will NOT prevent (as in they will not
         * "dominate") perhaps appearing other occurrences of same artifact under some other scope in the graph.
         */
        REMOVE
    }

    /**
     * Resolution scope: essentially "compile" and "runtime".
     */
    public static final class ResolutionScope extends Atom {
        public static final ResolutionScope NONE =
                new ResolutionScope("none", Collections.emptySet(), DependencyScope.ALL);
        public static final ResolutionScope EMPTY =
                new ResolutionScope("empty", Collections.emptySet(), Collections.emptySet());
        public static final ResolutionScope COMPILE = new ResolutionScope(
                "compile",
                Arrays.asList(DependencyScope.BOTH, DependencyScope.ONLY_COMPILE),
                Arrays.asList(DependencyScope.NONE, DependencyScope.ONLY_RUNTIME));
        public static final ResolutionScope RUNTIME = new ResolutionScope(
                "runtime",
                Arrays.asList(DependencyScope.BOTH, DependencyScope.ONLY_RUNTIME),
                Arrays.asList(DependencyScope.NONE, DependencyScope.ONLY_COMPILE));

        private final Set<DependencyScope> contains;
        private final Set<DependencyScope> excludesTransitively;

        public ResolutionScope(
                String id, Collection<DependencyScope> contains, Collection<DependencyScope> excludesTransitively) {
            super(id);
            this.contains = Collections.unmodifiableSet(new HashSet<>(contains));
            this.excludesTransitively = Collections.unmodifiableSet(new HashSet<>(excludesTransitively));
        }

        public Set<DependencyScope> getContains() {
            return contains;
        }

        public Set<DependencyScope> getExcludesTransitively() {
            return excludesTransitively;
        }

        public ResolutionScope plus(DependencyScope dependencyScope) {
            requireNonNull(dependencyScope);
            if (this == NONE) {
                throw new IllegalStateException("NONE is not extensible resolution scope");
            }
            if (getContains().contains(dependencyScope)) {
                return this;
            }
            if (this == EMPTY) {
                return new ResolutionScope(
                        dependencyScope.getId(), Collections.singleton(dependencyScope), Collections.emptySet());
            }
            HashSet<DependencyScope> dependencyScopes = new HashSet<>(getContains());
            dependencyScopes.add(dependencyScope);
            return new ResolutionScope(getId() + "+" + dependencyScope.getId(), dependencyScopes, excludesTransitively);
        }
    }

    /**
     * The language, that tells both "universes": language specific dependency scopes and resolution scopes.
     */
    public interface Language {
        String getId();

        Set<? extends LanguageDependencyScope> getLanguageDependencyScopeUniverse();

        Set<? extends LanguageResolutionScope> getLanguageResolutionScopeUniverse();
    }

    /**
     * Language dependency scope. Tells is it transitive, in which dependency scopes and which project scopes it is in.
     */
    public interface LanguageDependencyScope {
        String getId();

        Language getLanguage();

        boolean isTransitive();

        DependencyScope getDependencyScope();

        Set<ProjectScope> getProjectScopes();
    }

    /**
     * Language resolution scope. Tells which project scope, resolution scope and resolution mode is wanted.
     */
    public interface LanguageResolutionScope {
        String getId();

        Language getLanguage();

        ProjectScope getProjectScope();

        ResolutionScope getResolutionScope();

        ResolutionMode getResolutionMode();
    }
}
