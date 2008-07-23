package com.atlassian.plugin;

import java.io.File;

/**
 * @deprecated Since 2.0.0, use {@link JarPluginArtifact} instead
 */
public class FilePluginJar extends JarPluginArtifact implements PluginJar
{
    public FilePluginJar(File jarFile)
    {
        super(jarFile);
    }
}
