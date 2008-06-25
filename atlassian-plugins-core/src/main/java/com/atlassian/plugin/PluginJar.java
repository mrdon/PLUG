package com.atlassian.plugin;

import java.io.InputStream;

/**
 * Allows the retrieval of files or an input stream of a plugin JAR. Implementations
 * must allow multiple calls to {@link #getInputStream()}.
 *
 * @see PluginController
 */
public interface PluginJar
{
    /**
     * @return an input stream of the file specified inside the JAR.
     * @throws PluginParseException if the file was not found or could not be
     * read from the JAR.
     */
    InputStream getFile(String fileName) throws PluginParseException;

    /**
     * @return the original name of the plugin JAR file. Typically used
     * for persisting it to disk with a meaningful name.
     */
    String getFileName();

    /**
     * Returns an InputStream for the entire plugin JAR. Calling this
     * multiple times will return a fresh input stream each time.
     */
    InputStream getInputStream();
}
