package com.atlassian.plugin;

import java.io.InputStream;

/**
 * @deprecated Since 2.0.0, use {@link PluginArtifact}
 */
public interface PluginJar extends PluginArtifact
{
    /**
     * @return an input stream of the file specified inside the artifact.
     * @throws PluginParseException if the file was not found or could not be
     * read from the artifact.
     */
    InputStream getFile(String fileName) throws PluginParseException;

    /**
     * @return the original name of the plugin artifact file. Typically used
     * for persisting it to disk with a meaningful name.
     */
    String getFileName();
}
