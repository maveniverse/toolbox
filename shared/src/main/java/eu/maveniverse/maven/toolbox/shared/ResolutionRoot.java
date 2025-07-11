/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.toolbox.shared;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;

/**
 * Resolution root, that directs what we want to resolve (or collect).
 * <p>
 * To circumvent Resolver 1.x issue manifested with wrong Runtime graph building.
 * <p>
 * This class once constructed is immutable.
 */
public final class ResolutionRoot {
    private final Artifact artifact;
    private final boolean load;
    private final boolean applyManagedDependencies;
    private final boolean prepared;
    private final List<Dependency> dependencies;
    private final List<Dependency> managedDependencies;

    private ResolutionRoot(
            Artifact artifact,
            boolean load,
            boolean applyManagedDependencies,
            boolean prepared,
            List<Dependency> dependencies,
            List<Dependency> managedDependencies) {
        this.artifact = artifact;
        this.load = load;
        this.applyManagedDependencies = applyManagedDependencies;
        this.prepared = prepared;
        this.dependencies =
                dependencies.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(dependencies);
        this.managedDependencies = managedDependencies.isEmpty()
                ? Collections.emptyList()
                : Collections.unmodifiableList(managedDependencies);
    }

    /**
     * The artifact to resolve.
     */
    public Artifact getArtifact() {
        return artifact;
    }

    /**
     * Should its POM be loaded?
     */
    public boolean isLoad() {
        return load;
    }

    /**
     * Should managed dependencies be applied onto dependencies while preparing?
     */
    public boolean isApplyManagedDependencies() {
        return applyManagedDependencies;
    }

    /**
     * Is this instance prepared (for processing)?
     */
    public boolean isPrepared() {
        return prepared;
    }

    /**
     * To mark root as prepared (for processing).
     * <p>
     * Note: users should not invoke this method, or at least, should be aware of the consequences.
     */
    public ResolutionRoot prepared() {
        List<Dependency> newDependencies = dependencies;
        if (applyManagedDependencies) {
            newDependencies = new ArrayList<>(dependencies.size());
            for (Dependency dependency : dependencies) {
                if (dependency.isOptional()) {
                    continue;
                }
                String key = ArtifactIdUtils.toVersionlessId(dependency.getArtifact());
                Optional<Dependency> managed = managedDependencies.stream()
                        .filter(d -> key.equals(ArtifactIdUtils.toVersionlessId(d.getArtifact())))
                        .findFirst();
                if (managed.isPresent()) {
                    Dependency managedDependency = managed.orElseThrow();
                    if (!managedDependency.getArtifact().getVersion().trim().isEmpty()) {
                        dependency = dependency.setArtifact(dependency
                                .getArtifact()
                                .setVersion(managedDependency.getArtifact().getVersion()));
                    }
                    if (!managedDependency.getScope().trim().isEmpty()) {
                        dependency = dependency.setScope(managedDependency.getScope());
                    }
                    if (managedDependency.getOptional() != null) {
                        dependency = dependency.setOptional(managedDependency.getOptional());
                    }
                    if (!managedDependency.getExclusions().isEmpty()) {
                        dependency = dependency.setExclusions(managedDependency.getExclusions());
                    }
                }
                newDependencies.add(dependency);
            }
        }
        return new ResolutionRoot(artifact, load, false, true, newDependencies, managedDependencies);
    }

    /**
     * Explicitly specified direct dependencies (as immutable list).
     * <p>
     * If {@link #isLoad()} is {@code true}, these dependencies will be merged with loaded POM dependencies as
     * "dominant" ones. Otherwise, only these dependencies will be considered.
     */
    public List<Dependency> getDependencies() {
        return dependencies;
    }

    /**
     * Explicitly specified dependency management (as immutable list).
     * <p>
     * If {@link #isLoad()} is {@code true}, these dependency management entries will be merged with loaded POM
     * dependency management as "dominant" ones. Otherwise, only these dependency management entries will be considered.
     */
    public List<Dependency> getManagedDependencies() {
        return managedDependencies;
    }

    /**
     * Returns new instance of {@link Builder} initialized with this instance, never {@code null}.
     */
    public Builder builder() {
        return new Builder(artifact, load, applyManagedDependencies, dependencies, managedDependencies);
    }

    /**
     * Returns builder that treats artifact as existing, and will load up its POM to fill in details.
     */
    public static Builder ofLoaded(Artifact artifact) {
        return new Builder(artifact).load();
    }

    /**
     * Returns builder that treats artifact as not existing, and will not load up its POM. Returned builder is
     * incomplete, and must have further details filled in, like {@link Builder#withDependencies(List)}.
     */
    public static Builder ofNotLoaded(Artifact artifact) {
        return new Builder(artifact).doNotLoad();
    }

    public static final class Builder {
        private final Artifact artifact;
        private boolean load;
        private boolean applyManagedDependencies;
        private List<Dependency> dependencies;
        private List<Dependency> managedDependencies;

        private Builder(Artifact artifact) {
            this(artifact, false, false, Collections.emptyList(), Collections.emptyList());
        }

        private Builder(
                Artifact artifact,
                boolean load,
                boolean applyManagedDependencies,
                List<Dependency> dependencies,
                List<Dependency> managedDependencies) {
            this.artifact = requireNonNull(artifact, "artifact");
            this.load = load;
            this.applyManagedDependencies = applyManagedDependencies;
            this.dependencies = requireNonNull(dependencies, "dependencies");
            this.managedDependencies = requireNonNull(managedDependencies, "managedDependencies");
        }

        public Builder load() {
            this.load = true;
            return this;
        }

        public Builder doNotLoad() {
            this.load = false;
            return this;
        }

        public Builder applyManagedDependencies(boolean applyManagedDependencies) {
            this.applyManagedDependencies = applyManagedDependencies;
            return this;
        }

        public Builder withDependencies(List<Dependency> dependencies) {
            this.dependencies = new ArrayList<>(dependencies);
            return this;
        }

        public Builder withManagedDependencies(List<Dependency> managedDependencies) {
            this.managedDependencies = new ArrayList<>(managedDependencies);
            return this;
        }

        public ResolutionRoot build() {
            return new ResolutionRoot(
                    artifact, load, applyManagedDependencies, false, dependencies, managedDependencies);
        }
    }
}
