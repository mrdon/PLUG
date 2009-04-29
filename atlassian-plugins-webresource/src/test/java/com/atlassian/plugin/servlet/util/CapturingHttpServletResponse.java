package com.atlassian.plugin.servlet.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;

/**
* A {@link javax.servlet.http.HttpServletResponse} that captures its output (in platform specific charset!)
*
* @since v4.0
*/
public class CapturingHttpServletResponse implements HttpServletResponse
{
    private final ByteArrayOutputStream baos;
    private final ServletOutputStream servletOutputStream;
    private PrintWriter printWriter;

    public CapturingHttpServletResponse()
    {
        this.baos = new ByteArrayOutputStream();
        servletOutputStream = new ServletOutputStream() {

            @Override
            public void write(final int b) throws IOException
            {
                baos.write(b);
            }
        };

        printWriter = new PrintWriter(servletOutputStream);
    }


    @Override
    public ServletOutputStream getOutputStream() throws IOException
    {
        return servletOutputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException
    {
        return printWriter;
    }
    
    @Override
    public String toString()
    {
        return baos.toString();
    }

    //===================================================

    @Override
    public void addCookie(final Cookie cookie)
    {
    }

    @Override
    public boolean containsHeader(final String name)
    {
        return false;
    }

    @Override
    public String encodeURL(final String url)
    {
        return null;
    }

    @Override
    public String encodeRedirectURL(final String url)
    {
        return null;
    }

    @Override
    public String encodeUrl(final String url)
    {
        return null;
    }

    @Override
    public String encodeRedirectUrl(final String url)
    {
        return null;
    }

    @Override
    public void sendError(final int sc, final String msg) throws IOException
    {
    }

    @Override
    public void sendError(final int sc) throws IOException
    {
    }

    @Override
    public void sendRedirect(final String location) throws IOException
    {
    }

    @Override
    public void setDateHeader(final String name, final long date)
    {
    }

    @Override
    public void addDateHeader(final String name, final long date)
    {
    }

    @Override
    public void setHeader(final String name, final String value)
    {
    }

    @Override
    public void addHeader(final String name, final String value)
    {
    }

    @Override
    public void setIntHeader(final String name, final int value)
    {
    }

    @Override
    public void addIntHeader(final String name, final int value)
    {
    }

    @Override
    public void setStatus(final int sc)
    {
    }

    @Override
    public void setStatus(final int sc, final String sm)
    {
    }

    @Override
    public String getCharacterEncoding()
    {
        return null;
    }

    @Override
    public void setContentLength(final int len)
    {
    }

    @Override
    public void setContentType(final String type)
    {
    }

    @Override
    public void setBufferSize(final int size)
    {
    }

    @Override
    public int getBufferSize()
    {
        return 0;
    }

    @Override
    public void flushBuffer() throws IOException
    {
    }

    @Override
    public void resetBuffer()
    {
    }

    @Override
    public boolean isCommitted()
    {
        return false;
    }

    @Override
    public void reset()
    {
    }

    @Override
    public void setLocale(final Locale loc)
    {
    }

    @Override
    public Locale getLocale()
    {
        return null;
    }
}
