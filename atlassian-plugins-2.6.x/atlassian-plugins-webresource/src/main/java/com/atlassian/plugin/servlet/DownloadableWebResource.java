package com.atlassian.plugin.servlet;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.elements.ResourceLocation;

import java.io.InputStream;
import javax.servlet.ServletContext;

/**
 * A {@link DownloadableResource} that will serve the resource via the web application's {@link ServletContext}.
 */
public class DownloadableWebResource extends AbstractDownloadableResource
{
    private final ServletContext servletContext;

    public DownloadableWebResource(Plugin plugin, ResourceLocation resourceLocation, String extraPath, ServletContext servletContext, boolean disableMinification)
    {
        super(plugin, resourceLocation, extraPath, disableMinification);
        this.servletContext = servletContext;
    }

    @Override
    protected InputStream getResourceAsStream(final String resourceLocation)
    {
        return servletContext.getResourceAsStream(resourceLocation);
    }
}
