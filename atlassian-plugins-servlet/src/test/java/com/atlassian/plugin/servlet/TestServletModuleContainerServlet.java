package com.atlassian.plugin.servlet;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import com.atlassian.plugin.event.impl.DefaultPluginEventManager;
import com.atlassian.plugin.servlet.descriptors.ServletModuleDescriptor;
import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;

public class TestServletModuleContainerServlet extends TestCase
{
    // ensure that an UnavailableException thrown in the plugin servlet doesn't unload this servlet
    public void testServletDoesntUnloadItself() throws IOException, ServletException
    {
        ServletModuleDescriptor servletModuleDescriptor = new ServletModuleDescriptor()
        {
            protected void autowireObject(Object obj) {}
            protected ServletModuleManager getServletModuleManager() { return null; }
            public HttpServlet getModule() { return null; }
        };

        final DelegatingPluginServlet delegatingPluginServlet = new DelegatingPluginServlet(servletModuleDescriptor)
        {
            public void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException
            {
                throw new UnavailableException("Error in plugin servlet");
            }
        };

        final ServletModuleManager servletModuleManager = new DefaultServletModuleManager(new DefaultPluginEventManager())
        {
            public DelegatingPluginServlet getServlet(String path, ServletConfig servletConfig)
            {
                return delegatingPluginServlet;
            }
        };

        Mock mockHttpServletRequest = new Mock(HttpServletRequest.class);
        mockHttpServletRequest.matchAndReturn("getAttribute", C.anyArgs(1), null);
        mockHttpServletRequest.expectAndReturn("getPathInfo", "confluence");
        Mock mockHttpServletResponse = new Mock(HttpServletResponse.class);
        mockHttpServletResponse.expect("sendError", C.args(C.eq(500), C.isA(String.class)));

        ServletModuleContainerServlet servlet = new ServletModuleContainerServlet()
        {
            protected ServletModuleManager getServletModuleManager()
            {
                return servletModuleManager;
            }
        };

        servlet.service((HttpServletRequest) mockHttpServletRequest.proxy(), (HttpServletResponse) mockHttpServletResponse.proxy());
    }

    public void testIncludedServletDispatchesCorrectly() throws IOException, ServletException
    {
        final Mock mockHttpServletRequest = new Mock(HttpServletRequest.class);
        mockHttpServletRequest.matchAndReturn("getPathInfo", "/original");
        mockHttpServletRequest.expectAndReturn("getAttribute", "javax.servlet.include.path_info", "/included");
        final Mock mockHttpServletResponse = new Mock(HttpServletResponse.class);

        final MockHttpServlet originalServlet = new MockHttpServlet();
        final MockHttpServlet includedServlet = new MockHttpServlet();

        final ServletModuleManager servletModuleManager =
                new DefaultServletModuleManager(new DefaultPluginEventManager())
                {
                    @Override
                    public HttpServlet getServlet(String path, ServletConfig servletConfig) throws ServletException
                    {
                        if (path.equals("/original"))
                        {
                            return originalServlet;
                        }
                        else if (path.equals("/included"))
                        {
                            return includedServlet;
                        }
                        return null;
                    }
                };

        final ServletModuleContainerServlet servlet = new ServletModuleContainerServlet()
        {
            @Override
            protected ServletModuleManager getServletModuleManager()
            {
                return servletModuleManager;
            }
        };

        servlet.service((HttpServletRequest) mockHttpServletRequest.proxy(),
                (HttpServletResponse) mockHttpServletResponse.proxy());

        assertTrue("includedServlet should have been invoked", includedServlet.wasCalled);
        assertFalse("originalServlet should not have been invoked", originalServlet.wasCalled);
    }

    private static class MockHttpServlet extends HttpServlet
    {
        private boolean wasCalled = false;

        @Override
        protected void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
                throws ServletException, IOException
        {
            wasCalled = true;
        }
    }
}
