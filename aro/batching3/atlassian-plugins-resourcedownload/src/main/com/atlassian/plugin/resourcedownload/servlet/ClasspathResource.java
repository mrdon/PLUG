package com.atlassian.plugin.resourcedownload.servlet;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.resourcedownload.ContentTypeResolver;
import com.atlassian.plugin.elements.ResourceLocation;

import java.io.InputStream;

public class ClasspathResource extends AbstractDownloadableResource
{
    public ClasspathResource(Plugin plugin, ResourceLocation resourceLocation, String extraPath, ContentTypeResolver contentTypeResolver)
    {
        super(plugin, resourceLocation, extraPath, contentTypeResolver);
    }

    protected InputStream getResourceAsStream()
    {
        return plugin.getResourceAsStream(getLocation());
    }
}
