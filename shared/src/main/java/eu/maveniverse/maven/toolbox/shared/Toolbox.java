package eu.maveniverse.maven.toolbox.shared;

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
import org.eclipse.aether.util.artifact.JavaScopes;

public interface Toolbox {
    enum ResolutionScope {
        COMPILE(Arrays.asList(JavaScopes.RUNTIME, JavaScopes.TEST)),
        RUNTIME(Arrays.asList(JavaScopes.PROVIDED, JavaScopes.TEST)),
        TEST(Collections.emptyList());

        private final List<String> excludedScopes;

        ResolutionScope(List<String> excludedScopes) {
            this.excludedScopes = Collections.unmodifiableList(new ArrayList<>(excludedScopes));
        }

        public List<String> getExcludedScopes() {
            return excludedScopes;
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
}
