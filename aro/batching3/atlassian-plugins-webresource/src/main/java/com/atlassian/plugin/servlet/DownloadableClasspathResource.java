package com.atlassian.plugin.servlet;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.elements.ResourceLocation;

import java.io.InputStream;

public class DownloadableClasspathResource extends AbstractDownloadableResource
{
    public DownloadableClasspathResource(Plugin plugin, ResourceLocation resourceLocation, String extraPath)
    {
        super(plugin, resourceLocation, extraPath);
    }

    protected InputStream getResourceAsStream()
    {
        return plugin.getResourceAsStream(getLocation());
    }
}
