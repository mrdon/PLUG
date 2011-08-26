package com.atlassian.plugin.webresource.transformer;

import com.atlassian.plugin.servlet.DownloadException;
import com.atlassian.plugin.servlet.DownloadableResource;

import com.google.common.base.Function;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

public class TestTransformerUtils extends TestCase
{
    public void testTransform() throws Exception
    {
        final DownloadableResource resource = new MockDownloadableResource("hideho!");
        final StringCapture capture = new StringCapture();
        TransformerUtils.transformAndStreamResource(resource, Charset.forName("UTF-8"), capture.out(), new Function<CharSequence, CharSequence>()
        {
            public CharSequence apply(final CharSequence from)
            {
                System.out.println("transforming!!!");
                return "hoodeha?";
            }
        });
        assertEquals("hoodeha?", capture.toString());
    }
}

class StringCapture
{
    private final StringBuilder builder = new StringBuilder();

    OutputStream out()
    {
        return new WriterOutputStream(new AppendableWriter(builder));
    }

    @Override
    public String toString()
    {
        return builder.toString();
    }
}

class MockDownloadableResource implements DownloadableResource
{
    private final String content;

    MockDownloadableResource(final String content)
    {
        this.content = content;
    }

    public void streamResource(final OutputStream out) throws DownloadException
    {
        try
        {
            out.write(content.getBytes());
        }
        catch (final IOException e)
        {
            throw new DownloadException(e);
        }
    }

    public void serveResource(final HttpServletRequest request, final HttpServletResponse response) throws DownloadException
    {}

    public boolean isResourceModified(final HttpServletRequest request, final HttpServletResponse response)
    {
        return false;
    }

    public String getContentType()
    {
        return null;
    }

}