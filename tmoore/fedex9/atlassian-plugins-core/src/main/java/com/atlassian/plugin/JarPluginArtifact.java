package com.atlassian.plugin;

import java.io.File;

/**
 * @deprecated Since 2.1.0, use {@link com.atlassian.plugin.artifact.JarPluginArtifact} instead
 */
public class JarPluginArtifact extends com.atlassian.plugin.artifact.JarPluginArtifact
{
    public JarPluginArtifact(File file)
    {
        super(file);
    }
}
