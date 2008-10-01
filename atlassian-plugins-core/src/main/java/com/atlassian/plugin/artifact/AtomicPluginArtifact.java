package com.atlassian.plugin.artifact;

import com.atlassian.plugin.artifact.AbstractFilePluginArtifact;
import com.atlassian.plugin.PluginParseException;

import java.io.*;

/**
 * An XML plugin artifact that is just the atlassian-plugin.xml file
 *
 * @since 2.1.0
 */
class AtomicPluginArtifact extends AbstractFilePluginArtifact
{
    public AtomicPluginArtifact(File xmlFile)
    {
        super(xmlFile);
    }

    /**
     * Always returns null, since it doesn't make sense for an XML artifact
     */
    public InputStream getResourceAsStream(String name) throws PluginParseException
    {
        return null;
    }

}
