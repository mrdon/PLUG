package com.atlassian.plugin.webresource.transformer;

import com.atlassian.plugin.servlet.DownloadException;
import com.atlassian.plugin.servlet.DownloadableResource;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

/**
 * Abstract class for implementing downloadable resources that are wrapping an underlying resource as part of a
 * {@link WebResourceTransformer} implementation.  Subclasses are expected to implement {@link #streamResource(OutputStream)},
 * while the other methods are delegated by default.
 *
 * @since 2.5.0
 */
public abstract class AbstractTransformedDownloadableResource implements DownloadableResource
{
    private final DownloadableResource originalResource;

    public AbstractTransformedDownloadableResource(DownloadableResource originalResource) {
        this.originalResource = originalResource;
    }

    public boolean isResourceModified(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        return originalResource.isResourceModified(httpServletRequest, httpServletResponse);
    }

    public void serveResource(HttpServletRequest httpServletRequest, HttpServletResponse response) throws DownloadException
    {
        // Allow subclasses to override the content type
        final String contentType = getContentType();
        if (StringUtils.isNotBlank(contentType))
        {
            response.setContentType(contentType);
        }

        OutputStream out;
        try
        {
            out = response.getOutputStream();
        }
        catch (final IOException e)
        {
            throw new DownloadException(e);
        }

        streamResource(out);
    }

    public String getContentType() {
        return originalResource.getContentType();
    }
}
