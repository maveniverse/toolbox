package eu.maveniverse.maven.toolbox.shared;

/**
 * Label for "build path", like "compile", "runtime", etc.
 */
public interface BuildPath {
    /**
     * The label.
     */
    String getId();

    /**
     * Returns the "order" of this path, usable to sort against other instances.
     * Expected natural order is "compile", "runtime"... (basically like the processing order).
     */
    int order();
}
