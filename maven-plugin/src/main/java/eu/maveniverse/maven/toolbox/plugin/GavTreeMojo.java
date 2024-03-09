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
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
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
     * Set it {@code true} for verbose tree.
     */
    @Parameter(property = "verbose", defaultValue = "false", required = true)
    private boolean verbose;

    @Override
    public void execute() throws MojoExecutionException {
        Runtime runtime = Runtimes.INSTANCE.getRuntime();
        try (Context context = runtime.create(ContextOverrides.create().build())) {
            Toolbox toolbox = new ToolboxImpl(context);
            CollectResult collectResult = toolbox.collectAsDependency(
                    Toolbox.ResolutionScope.valueOf(scope.toUpperCase(Locale.ENGLISH)),
                    new Dependency(new DefaultArtifact(gav), scope),
                    context.remoteRepositories(),
                    verbose);
            collectResult.getRoot().accept(new DependencyGraphDumper(getLog()::info));
        } catch (DependencyCollectionException e) {
            throw new MojoExecutionException(e);
        }
    }
}
