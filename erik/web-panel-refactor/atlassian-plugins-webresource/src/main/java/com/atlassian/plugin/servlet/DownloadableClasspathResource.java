package com.atlassian.plugin.servlet;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.elements.ResourceLocation;

import java.io.InputStream;

/**
 * A {@link DownloadableResource} that will serve the resource from the plugin.
 * @see {@link Plugin#getResourceAsStream(String)}.
 */
public class DownloadableClasspathResource extends AbstractDownloadableResource
{
    public DownloadableClasspathResource(Plugin plugin, ResourceLocation resourceLocation, String extraPath)
    {
        super(plugin, resourceLocation, extraPath);
    }

    @Override
    protected InputStream getResourceAsStream(final String resourceLocation)
    {
        return plugin.getResourceAsStream(resourceLocation);
    }
}
