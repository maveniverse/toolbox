package eu.maveniverse.maven.toolbox.shared;

import eu.maveniverse.maven.toolbox.shared.internal.ScopeDependencySelector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

public interface Toolbox {
    enum ResolutionScope {
        COMPILE(
                Arrays.asList(JavaScopes.RUNTIME, JavaScopes.TEST),
                ScopeDependencySelector.fromRoot(null, Arrays.asList(JavaScopes.RUNTIME, JavaScopes.TEST))),
        COMPILE_PLUS_RUNTIME(
                Collections.singletonList(JavaScopes.TEST),
                ScopeDependencySelector.fromRoot(null, Collections.singletonList(JavaScopes.TEST))),
        RUNTIME(
                Arrays.asList(JavaScopes.PROVIDED, JavaScopes.TEST),
                ScopeDependencySelector.fromRoot(null, Arrays.asList(JavaScopes.PROVIDED, JavaScopes.TEST))),
        TEST(
                Collections.emptyList(),
                ScopeDependencySelector.fromDirect(null, Arrays.asList(JavaScopes.PROVIDED, JavaScopes.TEST)));

        private final List<String> directExcludedScopes;
        private final ScopeDependencySelector scopeDependencySelector;

        ResolutionScope(List<String> directExcludedScopes, ScopeDependencySelector scopeDependencySelector) {
            this.directExcludedScopes = Collections.unmodifiableList(new ArrayList<>(directExcludedScopes));
            this.scopeDependencySelector = scopeDependencySelector;
        }

        public List<String> getDirectExcludedScopes() {
            return directExcludedScopes;
        }

        public ScopeDependencySelector getScopeDependencySelector() {
            return scopeDependencySelector;
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
