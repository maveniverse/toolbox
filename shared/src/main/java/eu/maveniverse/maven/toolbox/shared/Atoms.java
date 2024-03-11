/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The "atoms" of the Toolbox that are used to assemble language specific dependency scopes and resolution scopes.
 * <p>
 * Essentially, we deal with 3-dimensional space: project scope, resolution scope, resolution mode. The languages
 * place their scopes into this space (and name them and select the transitive ones).
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
     * Project scope: like "main", "test", etc...
     */
    public static final class ProjectScope extends Atom {
        public static final ProjectScope MAIN = new ProjectScope("main");
        public static final ProjectScope TEST = new ProjectScope("test");

        public static final Set<ProjectScope> ALL =
                Collections.unmodifiableSet(new HashSet<>(Arrays.asList(MAIN, TEST)));

        private ProjectScope(String id) {
            super(id);
        }
    }

    /**
     * Resolution scope: "compile" and "runtime".
     */
    public static final class ResolutionScope extends Atom {
        public static final ResolutionScope COMPILE = new ResolutionScope("compile");
        public static final ResolutionScope RUNTIME = new ResolutionScope("runtime");

        public ResolutionScope(String id) {
            super(id);
        }
    }

    /**
     * Resolution mode: "eliminate" or (just) "remove".
     */
    public static final class ResolutionMode extends Atom {
        /**
         * Mode where artifacts in non-wanted scopes are eliminated. In other words, this mode ensures that if a
         * dependency was removed as it was in unwanted scope, it will guarantee that no such dependency will appear
         * anywhere else in the resulting graph.
         */
        public static final ResolutionMode ELIMINATE = new ResolutionMode("eliminate");

        /**
         * Mode where artifacts in non-wanted scopes are removed only. In other words, this mode will NOT prevent
         * (as in, removed will not "dominate") perhaps appearing other occurrences of same artifact under some other
         * scope in the graph.
         */
        public static final ResolutionMode REMOVE = new ResolutionMode("remove");

        private ResolutionMode(String id) {
            super(id);
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
        public static final DependencyScope NONE = new DependencyScope("none", Collections.emptySet());
        public static final DependencyScope BOTH =
                new DependencyScope("both", Arrays.asList(ResolutionScope.COMPILE, ResolutionScope.RUNTIME));
        public static final DependencyScope ONLY_RUNTIME =
                new DependencyScope("onlyRuntime", Collections.singleton(ResolutionScope.RUNTIME));
        public static final DependencyScope ONLY_COMPILE =
                new DependencyScope("onlyCompile", Collections.singleton(ResolutionScope.COMPILE));

        private final Set<ResolutionScope> memberOf;

        private DependencyScope(String id, Collection<ResolutionScope> resolutionScopes) {
            super(id);
            this.memberOf = Collections.unmodifiableSet(new HashSet<>(resolutionScopes));
        }

        public Set<ResolutionScope> getMemberOf() {
            return memberOf;
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

        Set<DependencyScope> getDependencyScopes();

        Set<ProjectScope> getProjectScopes();

        default Set<ResolutionScope> getMemberOf() {
            return getDependencyScopes().stream()
                    .map(DependencyScope::getMemberOf)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet());
        }
    }

    /**
     * Language resolution scope. Tells which project scope, resolution scope and resolution mode is wanted.
     */
    public interface LanguageResolutionScope {
        String getId();

        Language getLanguage();

        Set<ProjectScope> getProjectScopes();

        Set<ResolutionScope> getResolutionScopes();

        ResolutionMode getResolutionMode();
    }
}
