package com.atlassian.plugin;

import java.io.InputStream;

/**
 * Allows the retrieval of files and/or an input stream of a plugin artifact. Implementations
 * must allow multiple calls to {@link #getInputStream()}.
 *
 * @see PluginController
 */
public interface PluginArtifact
{
    /**
     * @return an input stream of the resource specified inside the artifact.
     * @throws PluginParseException if the resource was not found or could not be
     * read from the artifact.
     */
    InputStream getResourceAsStream(String name) throws PluginParseException;

    /**
     * @return the original name of the plugin artifact file. Typically used
     * for persisting it to disk with a meaningful name.
     */
    String getName();

    /**
     * Returns an InputStream for the entire plugin artifact. Calling this
     * multiple times will return a fresh input stream each time.
     */
    InputStream getInputStream();
}
