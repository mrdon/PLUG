package com.atlassian.plugin.descriptors.servlet;

import junit.framework.TestCase;
import com.mockobjects.dynamic.Mock;
import com.mockobjects.dynamic.C;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.UnavailableException;
import javax.servlet.ServletException;
import javax.servlet.ServletConfig;
import java.io.IOException;

public class TestServletModuleContainerServlet extends TestCase
{
    // ensure that an UnavailableException thrown in the plugin servlet doesn't unload this servlet
    public void testServletDoesntUnloadItself() throws IOException, ServletException
    {
        ServletModuleDescriptor servletModuleDescriptor = new ServletModuleDescriptor() {

            protected void autowireObject(Object obj)
            {
            }

            protected ServletModuleManager getServletModuleManager()
            {
                return null;
            }

            public HttpServlet getServlet()
            {
                return null;
            }
        };

        final DelegatingPluginServlet delegatingPluginServlet = new DelegatingPluginServlet(servletModuleDescriptor) {
            public void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException
            {
                throw new UnavailableException("Error in plugin servlet");
            }
        };

        final ServletModuleManager servletModuleManager = new ServletModuleManager() {
            public DelegatingPluginServlet getServlet(String path, ServletConfig servletConfig)
            {
                return delegatingPluginServlet;
            }
        };

        Mock mockHttpServletRequest = new Mock(HttpServletRequest.class);
        mockHttpServletRequest.expectAndReturn("getPathInfo", "confluence");
        Mock mockHttpServletResponse = new Mock(HttpServletResponse.class);
        mockHttpServletResponse.expect("sendError", C.args(C.eq(500), C.isA(String.class)));

        ServletModuleContainerServlet servlet = new ServletModuleContainerServlet() {
            protected ServletModuleManager getServletModuleManager()
            {
                return servletModuleManager;
            }
        };

        servlet.service((HttpServletRequest)mockHttpServletRequest.proxy(), (HttpServletResponse)mockHttpServletResponse.proxy());
    }

}
