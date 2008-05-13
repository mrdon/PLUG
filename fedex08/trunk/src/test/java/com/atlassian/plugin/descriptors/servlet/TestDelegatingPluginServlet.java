package com.atlassian.plugin.descriptors.servlet;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.impl.DynamicPlugin;
import com.atlassian.plugin.loaders.classloading.PluginsClassLoader;
import com.mockobjects.dynamic.Mock;

public class TestDelegatingPluginServlet extends TestCase
{
    PluginsClassLoader classLoader;
    Plugin plugin;
    Mock mockRequest;
    Mock mockResponse;
    
    List paths;
    
    public void setUp()
    {
        classLoader = new PluginsClassLoader(getClass().getClassLoader())
        {
            public Object clone() { return null; }
            protected URL getDataURL(String name, byte[] data) throws MalformedURLException { return null; }
            protected byte[] getFile(String path) { return null; }
        };
        plugin = new DynamicPlugin(null, classLoader);
        
        mockRequest = new Mock(HttpServletRequest.class);
        mockRequest.matchAndReturn("getPathInfo", "/plugin/servlet/*");
        mockResponse = new Mock(HttpServletResponse.class);
        
        paths = new ArrayList();
        paths.add("/name/servlet");
    }
    
    /**
     * Test to make sure the plugin class loader is set for the thread context class loader when init is called.
     */
    public void testInitCalledWithPluginClassLoaderAsThreadClassLoader() throws ServletException
    {
        HttpServlet wrappedServlet = new HttpServlet()
        {
            public void init(ServletConfig config)
            {
                assertSame(classLoader, Thread.currentThread().getContextClassLoader());
            }
        };

        getDelegatingServlet(wrappedServlet).init(null);
    }
    
    /**
     * Test to make sure the plugin class loader is set for the thread context class loader when service is called.
     */
    public void testServiceCalledWithPluginClassLoaderAsThreadClassLoader() throws ServletException, IOException
    {
        HttpServlet wrappedServlet = new HttpServlet()
        {
            public void service(HttpServletRequest request, HttpServletResponse response)
            {
                assertSame(classLoader, Thread.currentThread().getContextClassLoader());
            }
        };

        getDelegatingServlet(wrappedServlet).service((HttpServletRequest) mockRequest.proxy(), (HttpServletResponse) mockResponse.proxy());
    }
    
    /**
     * Test to make sure the servlet is called with our request wrapper.
     */
    public void testServiceCalledWithWrappedRequest() throws ServletException, IOException
    {
        HttpServlet wrappedServlet = new HttpServlet()
        {
            public void service(HttpServletRequest request, HttpServletResponse response)
            {
                assertTrue(request instanceof PluginHttpRequestWrapper);
            }
        };

        getDelegatingServlet(wrappedServlet).service((HttpServletRequest) mockRequest.proxy(), (HttpServletResponse) mockResponse.proxy());
    }

    private DelegatingPluginServlet getDelegatingServlet(HttpServlet wrappedServlet)
    {
        ServletModuleDescriptor descriptor = new MockServletModuleDescriptor(plugin, wrappedServlet, paths);
        return new DelegatingPluginServlet(descriptor);
    }
}
