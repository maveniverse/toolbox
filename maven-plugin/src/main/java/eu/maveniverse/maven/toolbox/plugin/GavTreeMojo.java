package eu.maveniverse.maven.toolbox.plugin;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.Runtime;
import eu.maveniverse.maven.mima.context.Runtimes;
import eu.maveniverse.maven.toolbox.shared.Toolbox;
import eu.maveniverse.maven.toolbox.shared.internal.ToolboxImpl;
import java.util.Locale;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.util.graph.visitor.DependencyGraphDumper;

@Mojo(name = "gav-tree", requiresProject = false, threadSafe = true)
public class GavTreeMojo extends AbstractMojo {
    /**
     * The artifact coordinates in the format {@code <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>}
     * to display tree for.
     */
    @Parameter(property = "gav", required = true)
    private String gav;

    /**
     * The resolution scope to display, accepted values are "runtime", "compile" or "test".
     */
    @Parameter(property = "scope", defaultValue = "runtime", required = true)
    private String scope;

    /**
     * Set it {@code true} for "as project" mode.
     */
    @Parameter(property = "asProject", defaultValue = "false", required = true)
    private boolean asProject;

    /**
     * Set it {@code true} for verbose tree.
     */
    @Parameter(property = "verbose", defaultValue = "false", required = true)
    private boolean verbose;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Runtime runtime = Runtimes.INSTANCE.getRuntime();
        try (Context context = runtime.create(ContextOverrides.create().build())) {
            Toolbox toolbox = new ToolboxImpl(context.repositorySystem());
            Toolbox.ResolutionScope resolutionScope =
                    Toolbox.ResolutionScope.valueOf(scope.toUpperCase(Locale.ENGLISH));
            CollectResult collectResult;
            if (asProject) {
                Artifact artifact = new DefaultArtifact(gav);

                // for test only: "as project" means we use MavenProject, here it is just simulated
                ArtifactDescriptorRequest descriptorRequest = new ArtifactDescriptorRequest();
                descriptorRequest.setArtifact(artifact);
                descriptorRequest.setRepositories(context.remoteRepositories());
                ArtifactDescriptorResult descriptorResult = context.repositorySystem()
                        .readArtifactDescriptor(context.repositorySystemSession(), descriptorRequest);

                collectResult = toolbox.collectAsProject(
                        context.repositorySystemSession(),
                        resolutionScope,
                        artifact,
                        descriptorResult.getDependencies(),
                        descriptorResult.getManagedDependencies(),
                        descriptorResult.getRepositories(),
                        verbose);
            } else {
                collectResult = toolbox.collectAsDependency(
                        context.repositorySystemSession(),
                        resolutionScope,
                        new Dependency(new DefaultArtifact(gav), scope),
                        context.remoteRepositories(),
                        verbose);
            }
            collectResult.getRoot().accept(new DependencyGraphDumper(getLog()::info));
        } catch (ArtifactDescriptorException | DependencyCollectionException e) {
            throw new MojoExecutionException(e);
        }
    }
}
