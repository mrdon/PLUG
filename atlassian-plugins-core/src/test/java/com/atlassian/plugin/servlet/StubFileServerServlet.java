package com.atlassian.plugin.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;

class StubFileServerServlet extends BaseFileServerServlet
{
    private final DownloadStrategy downloadStrategy;
    private final String contentType;

    public StubFileServerServlet(DownloadStrategy downloadStrategy, String contentType)
    {
        this.downloadStrategy = downloadStrategy;
        this.contentType = contentType;
    }

    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException
    {
        downloadStrategy.serveFile(this, httpServletRequest, httpServletResponse);
    }

    public String getDecodedPathInfo(HttpServletRequest httpServletRequest)
    {
        return httpServletRequest.getPathInfo();
    }

    protected DownloadStrategy instantiateDownloadStrategy(Class downloadStrategyClass)
    {
        return null;
    }

    protected String urlDecode(String url)
    {
        return url;
    }

    protected String getContentType(String location)
    {
        return contentType;
    }
}
