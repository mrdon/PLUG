package com.atlassian.plugin;

import java.io.File;
import java.io.InputStream;

/**
 * @deprecated Since 2.0.0, use {@link JarPluginArtifact} instead
 */
public class FilePluginJar extends JarPluginArtifact implements PluginJar
{
    public FilePluginJar(File jarFile)
    {
        super(jarFile);
    }

    public InputStream getFile(String fileName) throws PluginParseException
    {
        return getResourceAsStream(fileName);
    }

    public String getFileName()
    {
        return getName();
    }
}
