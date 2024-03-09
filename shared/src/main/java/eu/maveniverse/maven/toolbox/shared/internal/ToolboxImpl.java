package eu.maveniverse.maven.toolbox.shared.internal;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.toolbox.shared.Toolbox;
import java.util.ArrayList;
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
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.eclipse.aether.util.graph.visitor.CloningDependencyVisitor;
import org.eclipse.aether.util.graph.visitor.FilteringDependencyVisitor;
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
                resolutionScope.getScopeDependencySelector(),
                OptionalDependencySelector.fromDirect(),
                new ExclusionDependencySelector()));

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRootArtifact(root);
        collectRequest.setDependencies(dependencies);
        collectRequest.setManagedDependencies(managedDependencies);
        collectRequest.setRepositories(remoteRepositories);
        collectRequest.setRequestContext("project");
        collectRequest.setTrace(RequestTrace.newChild(null, collectRequest));

        logger.debug("Collecting {}", collectRequest);
        CollectResult result = repositorySystem.collectDependencies(session, collectRequest);
        if (resolutionScope.getScopeDependencyFilter() != null) {
            CloningDependencyVisitor cloning = new CloningDependencyVisitor();
            FilteringDependencyVisitor filter =
                    new FilteringDependencyVisitor(cloning, resolutionScope.getScopeDependencyFilter());
            result.getRoot().accept(filter);
            result.setRoot(cloning.getRootNode());
        }
        return result;
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
                resolutionScope.getScopeDependencySelector(),
                OptionalDependencySelector.fromDirect(),
                new ExclusionDependencySelector()));

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(root);
        collectRequest.setRepositories(remoteRepositories);
        collectRequest.setRequestContext("project");
        collectRequest.setTrace(RequestTrace.newChild(null, collectRequest));

        logger.debug("Collecting {}", collectRequest);
        CollectResult result = repositorySystem.collectDependencies(session, collectRequest);
        if (resolutionScope.getScopeDependencyFilter() != null) {
            CloningDependencyVisitor cloning = new CloningDependencyVisitor();
            FilteringDependencyVisitor filter =
                    new FilteringDependencyVisitor(cloning, resolutionScope.getScopeDependencyFilter());
            result.getRoot().accept(filter);
            result.setRoot(cloning.getRootNode());
        }
        return result;
    }

    @Override
    public List<ArtifactResult> resolveArtifacts(
            RepositorySystemSession repositorySystemSession,
            List<Artifact> artifacts,
            List<RemoteRepository> remoteRepositories)
            throws ArtifactResolutionException {
        requireNonNull(repositorySystemSession);
        requireNonNull(artifacts);

        List<ArtifactRequest> artifactRequests = new ArrayList<>();
        artifacts.forEach(a -> artifactRequests.add(new ArtifactRequest(a, remoteRepositories, null)));
        return repositorySystem.resolveArtifacts(repositorySystemSession, artifactRequests);
    }
}
