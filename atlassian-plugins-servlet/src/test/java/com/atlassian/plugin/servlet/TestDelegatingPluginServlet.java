package com.atlassian.plugin.servlet;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.classloader.PluginClassLoader;
import com.atlassian.plugin.impl.DefaultDynamicPlugin;
import com.atlassian.plugin.servlet.descriptors.MockServletModuleDescriptor;
import com.atlassian.plugin.servlet.descriptors.ServletModuleDescriptor;
import com.atlassian.plugin.test.PluginTestUtils;
import com.mockobjects.dynamic.Mock;

public class TestDelegatingPluginServlet extends TestCase
{
    private PluginClassLoader classLoader;
    private Plugin plugin;
    private Mock mockRequest;
    private Mock mockResponse;
    private List<String> paths;
    
    public void setUp() throws Exception
    {
        classLoader = new PluginClassLoader(PluginTestUtils.getFileForResource(PluginTestUtils.SIMPLE_TEST_JAR));
        plugin = new DefaultDynamicPlugin(null, classLoader);
        
        mockRequest = new Mock(HttpServletRequest.class);
        mockRequest.matchAndReturn("getPathInfo", "/plugin/servlet/*");
        mockResponse = new Mock(HttpServletResponse.class);
        
        paths = new ArrayList<String>();
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
