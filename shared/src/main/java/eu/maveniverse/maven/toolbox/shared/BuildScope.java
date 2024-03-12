package eu.maveniverse.maven.toolbox.shared;

/**
 * Build scope is certain combination of {@link ProjectPath} and {@link BuildPath}.
 */
public interface BuildScope {
    /**
     * The label.
     */
    String getId();

    /**
     * The project path this scope belongs to.
     */
    ProjectPath getProjectPath();

    /**
     * The build path this scope belongs to.
     */
    BuildPath getBuildPath();
}
