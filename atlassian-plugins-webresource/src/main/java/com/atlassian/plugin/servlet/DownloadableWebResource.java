package com.atlassian.plugin.servlet;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.elements.ResourceLocation;

import javax.servlet.ServletContext;
import java.io.InputStream;

/**
 * A {@link DownloadableResource} that will serve the resource via the web application's {@link ServletContext}.
 */
public class DownloadableWebResource extends AbstractDownloadableResource
{
    private final ServletContext servletContext;

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
