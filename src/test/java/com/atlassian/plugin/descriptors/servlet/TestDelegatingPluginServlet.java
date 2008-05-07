package com.atlassian.plugin.descriptors.servlet;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.classloader.PluginClassLoader;
import com.atlassian.plugin.impl.DynamicPlugin;
import com.mockobjects.dynamic.Mock;
import junit.framework.TestCase;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class TestDelegatingPluginServlet extends TestCase
{
    PluginClassLoader classLoader;
    Plugin plugin;
    Mock mockRequest;
    Mock mockResponse;
    
    List paths;
    
    public void setUp() throws Exception
    {
        URL resource = getClass().getClassLoader().getResource("testjars/atlassian-plugins-simpletest-1.0.jar");
        classLoader = new PluginClassLoader(new File(resource.toURI()));
        plugin = new DynamicPlugin(null, classLoader);
        
        mockRequest = new Mock(HttpServletRequest.class);
        mockRequest.matchAndReturn("getPathInfo", "/plugin/servlet/*");
        mockResponse = new Mock(HttpServletResponse.class);
        
        paths = new ArrayList();
        paths.add("/name/servlet");
    }
    
    /**
     * Test to make sure the plugin class loader is set for the thread context class loader when init is called.
     * @throws Exception on test error
     */
    public void testInitCalledWithPluginClassLoaderAsThreadClassLoader() throws Exception
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
     * @throws Exception on test error
     */
    public void testServiceCalledWithPluginClassLoaderAsThreadClassLoader() throws Exception
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
     * @throws Exception on test error
     */
    public void testServiceCalledWithWrappedRequest() throws Exception
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
