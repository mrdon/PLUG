package com.atlassian.plugin.servlet;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;

/**
 *
 */
public abstract class BaseFileServerServlet extends HttpServlet
{

    public static final String PATH_SEPARATOR = "/";
    public static final String RESOURCE_URL_PREFIX = "resources";

    private static List downloadStrategies = Collections.synchronizedList(new ArrayList());
    private static final Log log = LogFactory.getLog(BaseFileServerServlet.class);

    static
    {
        downloadStrategies.add(PluginResourceDownload.class);
    }

    public static String SERVLET_PATH = "download";

    //~ Methods --------------------------------------------------------------------------------------------------------

    public void init() throws ServletException
    {
        super.init();
    }

    public void init(ServletConfig servletConfig) throws ServletException
    {
        super.init(servletConfig);
    }

    public String getMimeType(File fileToServe)
    {
        return getServletContext().getMimeType(fileToServe.getAbsolutePath());
    }

    /**
     * @deprecated Use {@link ResourceDownloadUtils#serveFileImpl(javax.servlet.http.HttpServletResponse, java.io.InputStream)} instead
     */
    public void serveFileImpl(HttpServletResponse httpServletResponse, InputStream in) throws IOException
    {
        ResourceDownloadUtils.serveFileImpl(httpServletResponse, in);
    }

    public abstract String getDecodedPathInfo(HttpServletRequest httpServletRequest);

    protected abstract DownloadStrategy instantiateDownloadStrategy(Class downloadStrategyClass);

    protected abstract String urlDecode(String url);

    protected abstract String getContentType(String location);

    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws ServletException, IOException
    {

        try
        {
            DownloadStrategy downloadStrategy = getDownloadStrategy(httpServletRequest);

            if (downloadStrategy != null)
            {
                downloadStrategy.serveFile(this, httpServletRequest, httpServletResponse);
            }
            else
            {
                httpServletResponse.sendError(HttpServletResponse.SC_NOT_FOUND, "The file you were looking for was not found");
            }
        }
        catch (Throwable t)
        {
            log.info("Error while serving file ", t);
            throw new ServletException(t);
        }
    }

    protected void addDownloadStrategy(Class strategyClass)
    {
        downloadStrategies.add(strategyClass);
    }

    private DownloadStrategy getDownloadStrategy(HttpServletRequest httpServletRequest)
    {
        String url = httpServletRequest.getRequestURI().toLowerCase();
        for (Iterator iterator = downloadStrategies.iterator(); iterator.hasNext();)
        {
            Class downloadStrategyClass = (Class) iterator.next();
            DownloadStrategy downloadStrategy = instantiateDownloadStrategy(downloadStrategyClass);
            if (downloadStrategy.matches(url))
            {
                return downloadStrategy;
            }
        }

        return null;
    }

}