package eu.maveniverse.maven.toolbox.plugin;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.Runtime;
import eu.maveniverse.maven.mima.context.Runtimes;
import eu.maveniverse.maven.toolbox.shared.Toolbox;
import eu.maveniverse.maven.toolbox.shared.internal.ToolboxImpl;
import java.util.Locale;
import java.util.stream.Collectors;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.util.graph.visitor.DependencyGraphDumper;

@Mojo(name = "tree", threadSafe = true)
public class TreeMojo extends AbstractMojo {
    /**
     * The resolution scope to display, accepted values are "runtime", "compile" or "test".
     */
    @Parameter(property = "scope", defaultValue = "runtime", required = true)
    private String scope;

    /**
     * Set it {@code true} for verbose tree.
     */
    @Parameter(property = "verbose", defaultValue = "false", required = true)
    private boolean verbose;

    @Component
    private MavenProject mavenProject;

    @Override
    public void execute() throws MojoExecutionException {
        Runtime runtime = Runtimes.INSTANCE.getRuntime();
        try (Context context = runtime.create(ContextOverrides.create().build())) {
            ArtifactTypeRegistry artifactTypeRegistry =
                    context.repositorySystemSession().getArtifactTypeRegistry();
            Toolbox toolbox = new ToolboxImpl(context.repositorySystem());
            Toolbox.ResolutionScope resolutionScope =
                    Toolbox.ResolutionScope.valueOf(scope.toUpperCase(Locale.ENGLISH));
            CollectResult collectResult = toolbox.collectAsProject(
                    context.repositorySystemSession(),
                    resolutionScope,
                    RepositoryUtils.toArtifact(mavenProject.getArtifact()),
                    mavenProject.getDependencies().stream()
                            .map(d -> RepositoryUtils.toDependency(d, artifactTypeRegistry))
                            .collect(Collectors.toList()),
                    mavenProject.getDependencyManagement().getDependencies().stream()
                            .map(d -> RepositoryUtils.toDependency(d, artifactTypeRegistry))
                            .collect(Collectors.toList()),
                    context.remoteRepositories(),
                    verbose);
            collectResult.getRoot().accept(new DependencyGraphDumper(getLog()::info));
        } catch (DependencyCollectionException e) {
            throw new MojoExecutionException(e);
        }
    }
}
