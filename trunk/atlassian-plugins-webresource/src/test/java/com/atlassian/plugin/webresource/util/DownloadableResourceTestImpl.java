package com.atlassian.plugin.webresource.util;

import com.atlassian.plugin.servlet.DownloadableResource;
import com.atlassian.plugin.servlet.DownloadException;

import java.io.IOException;
import java.io.OutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A implementation of {@link com.atlassian.plugin.servlet.DownloadableResource} for testing purposes
 */
public class DownloadableResourceTestImpl implements DownloadableResource
{
    private final String contentType;
    private final String content;


    public DownloadableResourceTestImpl(final String contentType, final String content)
    {
        this.contentType = contentType;
        this.content = content;
    }

    public boolean isResourceModified(final HttpServletRequest request, final HttpServletResponse response)
    {
        return false;
    }

    public void serveResource(final HttpServletRequest request, final HttpServletResponse response)
            throws DownloadException
    {
        try
        {
            writeContent(response.getOutputStream());
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void streamResource(final OutputStream out)
    {
        writeContent(out);
    }

    private void writeContent(final OutputStream out)
    {
        byte[] bytes = content.getBytes();
        try
        {
            out.write(bytes);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public String getContentType()
    {
        return contentType;
    }
}
