package eu.maveniverse.maven.toolbox.shared.internal;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.toolbox.shared.Toolbox;
import java.util.List;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;
import org.eclipse.aether.util.graph.selector.OptionalDependencySelector;
import org.eclipse.aether.util.graph.selector.ScopeDependencySelector;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToolboxImpl implements Toolbox {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final RepositorySystem repositorySystem;

    public ToolboxImpl(RepositorySystem repositorySystem) {
        this.repositorySystem = requireNonNull(repositorySystem);
    }

    @Override
    public CollectResult collectAsProject(
            RepositorySystemSession repositorySystemSession,
            ResolutionScope resolutionScope,
            Artifact root,
            List<Dependency> dependencies,
            List<Dependency> managedDependencies,
            List<RemoteRepository> remoteRepositories,
            boolean verbose)
            throws DependencyCollectionException {
        requireNonNull(repositorySystemSession);
        requireNonNull(resolutionScope);

        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession(repositorySystemSession);
        if (verbose) {
            session.setConfigProperty(ConflictResolver.CONFIG_PROP_VERBOSE, ConflictResolver.Verbosity.FULL);
            session.setConfigProperty(DependencyManagerUtils.CONFIG_PROP_VERBOSE, true);
        }
        session.setDependencySelector(new AndDependencySelector(
                new ScopeDependencySelector(null, resolutionScope.getExcludedScopes()),
                new OptionalDependencySelector(),
                new ExclusionDependencySelector()));

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRootArtifact(root);
        collectRequest.setDependencies(dependencies);
        collectRequest.setManagedDependencies(managedDependencies);
        collectRequest.setRepositories(remoteRepositories);
        collectRequest.setRequestContext("project");
        collectRequest.setTrace(RequestTrace.newChild(null, collectRequest));

        logger.debug("Collecting {}", collectRequest);
        return repositorySystem.collectDependencies(session, collectRequest);
    }

    @Override
    public CollectResult collectAsDependency(
            RepositorySystemSession repositorySystemSession,
            ResolutionScope resolutionScope,
            Dependency root,
            List<RemoteRepository> remoteRepositories,
            boolean verbose)
            throws DependencyCollectionException {
        requireNonNull(repositorySystemSession);
        requireNonNull(resolutionScope);
        requireNonNull(root);

        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession(repositorySystemSession);
        if (verbose) {
            session.setConfigProperty(ConflictResolver.CONFIG_PROP_VERBOSE, ConflictResolver.Verbosity.FULL);
            session.setConfigProperty(DependencyManagerUtils.CONFIG_PROP_VERBOSE, true);
        }
        session.setDependencySelector(new AndDependencySelector(
                new ScopeDependencySelector(null, resolutionScope.getExcludedScopes()),
                new OptionalDependencySelector(),
                new ExclusionDependencySelector()));

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(root);
        collectRequest.setRepositories(remoteRepositories);
        collectRequest.setRequestContext("project");
        collectRequest.setTrace(RequestTrace.newChild(null, collectRequest));

        logger.debug("Collecting {}", collectRequest);
        return repositorySystem.collectDependencies(session, collectRequest);
    }
}
