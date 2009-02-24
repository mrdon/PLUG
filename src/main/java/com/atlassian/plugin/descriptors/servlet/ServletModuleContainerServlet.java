package com.atlassian.plugin.descriptors.servlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import java.io.IOException;

public abstract class ServletModuleContainerServlet extends HttpServlet
{
    private static final Log log = LogFactory.getLog(ServletModuleContainerServlet.class);
    private ServletConfig servletConfig;

    public void init(ServletConfig servletConfig) throws ServletException
    {
        super.init(servletConfig);
        this.servletConfig = servletConfig;
    }

    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        if (getServletModuleManager() == null)
        {
            log.error("Could not get ServletModuleManager?");
            response.sendError(500, "Could not get ServletModuleManager.");
            return;
        }

        HttpServlet servlet = getServletModuleManager().getServlet(getPathInfo(request), servletConfig);

        if (servlet != null)
        {
            servlet.service(request, response);
            return;
        }
        else
        {
            log.debug("No servlet found for: " + getRequestURI(request));
            response.sendError(404, "Could not find servlet for: " + getRequestURI(request));
        }
    }

    /**
     * Retrieve the ServletModuleManager from your container framework.
     */
    protected abstract ServletModuleManager getServletModuleManager();

    private String getPathInfo(HttpServletRequest request)
    {
        String pathInfo = (String) request.getAttribute(RequestAttributes.PATH_INFO);
        if (pathInfo == null)
        {
            pathInfo = request.getPathInfo();
        }
        return pathInfo;
    }

    private String getRequestURI(HttpServletRequest request)
    {
        String requestURI = (String) request.getAttribute(RequestAttributes.REQUEST_URI);
        if (requestURI == null)
        {
            requestURI = request.getRequestURI();
        }
        return requestURI;
    }

    private static class RequestAttributes
    {
        static final String PATH_INFO = "javax.servlet.include.path_info";
        static final String REQUEST_URI = "javax.servlet.include.request_uri";
    }
}