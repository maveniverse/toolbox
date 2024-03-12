package eu.maveniverse.maven.toolbox.shared;

/**
 * Label for "project path", like "main", "test", but could be "it", etc.
 */
public interface ProjectPath {
    /**
     * The label.
     */
    String getId();

    /**
     * Returns the "order" of this path, usable to sort against other instances.
     * Expected natural order is "main", "test"... (basically like the processing order).
     */
    int order();
}
