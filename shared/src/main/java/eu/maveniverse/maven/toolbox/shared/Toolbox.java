package eu.maveniverse.maven.toolbox.shared;

import eu.maveniverse.maven.toolbox.shared.internal.ScopeDependencySelector;
import java.util.Arrays;
import java.util.List;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.ScopeDependencyFilter;

public interface Toolbox {
    enum ResolutionScope {
        COMPILE(
                ScopeDependencySelector.fromDirect(null, Arrays.asList(JavaScopes.RUNTIME, JavaScopes.TEST)),
                new ScopeDependencyFilter(null, Arrays.asList(JavaScopes.RUNTIME, JavaScopes.TEST))),
        RUNTIME(ScopeDependencySelector.fromRoot(null, Arrays.asList(JavaScopes.PROVIDED, JavaScopes.TEST)), null),
        TEST(ScopeDependencySelector.fromDirect(null, Arrays.asList(JavaScopes.PROVIDED, JavaScopes.TEST)), null);

        private final ScopeDependencySelector scopeDependencySelector;
        private final ScopeDependencyFilter scopeDependencyFilter;

        ResolutionScope(ScopeDependencySelector scopeDependencySelector, ScopeDependencyFilter scopeDependencyFilter) {
            this.scopeDependencySelector = scopeDependencySelector;
            this.scopeDependencyFilter = scopeDependencyFilter;
        }

        public ScopeDependencySelector getScopeDependencySelector() {
            return scopeDependencySelector;
        }

        public ScopeDependencyFilter getScopeDependencyFilter() {
            return scopeDependencyFilter;
        }
    }

    /**
     * Collects given (maybe even non-existent) {@link Artifact} with all the specified dependencies, managed
     * dependencies for given resolution scope.
     */
    CollectResult collectAsProject(
            RepositorySystemSession repositorySystemSession,
            ResolutionScope resolutionScope,
            Artifact root,
            List<Dependency> dependencies,
            List<Dependency> managedDependencies,
            List<RemoteRepository> remoteRepositories,
            boolean verbose)
            throws DependencyCollectionException;

    /**
     * Collects given existing {@link Dependency} by reusing POM information for given resolution scope.
     */
    CollectResult collectAsDependency(
            RepositorySystemSession repositorySystemSession,
            ResolutionScope resolutionScope,
            Dependency root,
            List<RemoteRepository> remoteRepositories,
            boolean verbose)
            throws DependencyCollectionException;

    /**
     * Resolves given artifacts from given remote repositories.
     */
    List<ArtifactResult> resolveArtifacts(
            RepositorySystemSession repositorySystemSession,
            List<Artifact> artifacts,
            List<RemoteRepository> remoteRepositories)
            throws ArtifactResolutionException;
}
