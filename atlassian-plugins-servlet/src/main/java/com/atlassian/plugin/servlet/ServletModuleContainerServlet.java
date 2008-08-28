package com.atlassian.plugin.servlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;

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
            log.error("Could not get DefaultServletModuleManager?");
            response.sendError(500, "Could not get DefaultServletModuleManager.");
            return;
        }

        HttpServlet servlet = getServletModuleManager().getServlet(request.getPathInfo(), servletConfig);

        if (servlet == null)
        {
            log.debug("No servlet found for: " + request.getRequestURI());
            response.sendError(404, "Could not find servlet for: " + request.getRequestURI());
            return;
        }

        try
        {
            servlet.service(request, response);
        }
        catch (UnavailableException e) // prevent this servlet from unloading itself (PLUG-79)
        {
            log.error(e);
            response.sendError(500, e.getMessage());
        }
        catch (ServletException e)
        {
            log.error(e);
            response.sendError(500, e.getMessage());
        }
    }

    /**
     * Retrieve the DefaultServletModuleManager from your container framework.
     */
    protected abstract ServletModuleManager getServletModuleManager();
}