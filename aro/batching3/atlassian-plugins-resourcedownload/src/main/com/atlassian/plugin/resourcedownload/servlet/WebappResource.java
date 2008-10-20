package com.atlassian.plugin.resourcedownload.servlet;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.resourcedownload.servlet.AbstractDownloadableResource;
import com.atlassian.plugin.resourcedownload.ContentTypeResolver;
import com.atlassian.plugin.elements.ResourceLocation;

import javax.servlet.ServletContext;
import java.io.InputStream;

public class WebappResource extends AbstractDownloadableResource
{
    private ServletContext servletContext;

    public WebappResource(Plugin plugin, ResourceLocation resourceLocation, String extraPath,
        ContentTypeResolver contentTypeResolver, ServletContext servletContext)
    {
        super(plugin, resourceLocation, extraPath, contentTypeResolver);
        this.servletContext = servletContext;
    }

    protected InputStream getResourceAsStream()
    {
        return servletContext.getResourceAsStream(getLocation());
    }
}
