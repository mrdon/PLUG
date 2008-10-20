package com.atlassian.plugin.servlet;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.elements.ResourceLocation;

import javax.servlet.ServletContext;
import java.io.InputStream;

public class DownloadableWebResource extends AbstractDownloadableResource
{
    private ServletContext servletContext;

    public DownloadableWebResource(Plugin plugin, ResourceLocation resourceLocation, String extraPath, ServletContext servletContext)
    {
        super(plugin, resourceLocation, extraPath);
        this.servletContext = servletContext;
    }

    protected InputStream getResourceAsStream()
    {
        return servletContext.getResourceAsStream(getLocation());
    }
}
