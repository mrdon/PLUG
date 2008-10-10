package com.atlassian.plugin.servlet.filter;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.artifact.JarPluginArtifact;
import com.atlassian.plugin.classloader.PluginClassLoader;
import com.atlassian.plugin.impl.DefaultDynamicPlugin;
import com.atlassian.plugin.servlet.descriptors.ServletFilterModuleDescriptor;
import com.atlassian.plugin.servlet.descriptors.ServletFilterModuleDescriptorBuilder;
import com.atlassian.plugin.servlet.filter.FilterTestUtils.FilterAdapter;
import static com.atlassian.plugin.servlet.filter.FilterTestUtils.emptyChain;
import static com.atlassian.plugin.servlet.filter.FilterTestUtils.newList;
import static com.atlassian.plugin.test.PluginTestUtils.FILTER_TEST_JAR;
import static com.atlassian.plugin.test.PluginTestUtils.getFileForResource;
import com.mockobjects.dynamic.Mock;
import junit.framework.TestCase;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;

public class TestDelegatingPluginFilter extends TestCase
{
    public void testPluginClassLoaderIsThreadContextClassLoaderWhenFiltering() throws Exception
    {
        Mock mockRequest = new Mock(HttpServletRequest.class);
        mockRequest.expectAndReturn("getPathInfo", "/servlet");
        Mock mockResponse = new Mock(HttpServletResponse.class);

        createClassLoaderCheckingFilter("filter").doFilter((HttpServletRequest) mockRequest.proxy(), (HttpServletResponse) mockResponse.proxy(), emptyChain);
    }
    
    public void testPluginClassLoaderIsThreadContextLoaderWhenFiltersInChainAreFromDifferentPlugins() throws Exception
    {
        Mock mockRequest = new Mock(HttpServletRequest.class);
        mockRequest.matchAndReturn("getPathInfo", "/servlet");
        Mock mockResponse = new Mock(HttpServletResponse.class);

        Iterable<Filter> filters = newList(
            createClassLoaderCheckingFilter("filter-1"), 
            createClassLoaderCheckingFilter("filter-2"), 
            createClassLoaderCheckingFilter("filter-3")
        );
        FilterChain chain = new IteratingFilterChain(filters.iterator(), emptyChain);
        chain.doFilter((HttpServletRequest) mockRequest.proxy(), (HttpServletResponse) mockResponse.proxy());
    }
    
    public void testPluginClassLoaderIsRestoredProperlyWhenAnExceptionIsThrownFromFilter() throws Exception
    {
        Mock mockRequest = new Mock(HttpServletRequest.class);
        mockRequest.matchAndReturn("getPathInfo", "/servlet");
        Mock mockResponse = new Mock(HttpServletResponse.class);

        Iterable<Filter> filters = newList(
            createClassLoaderCheckingFilter("filter-1"), 
            createClassLoaderCheckingFilter("filter-2"),
            createExceptionThrowingFilter("exception-filter"),
            createClassLoaderCheckingFilter("filter-3")
        );
        FilterChain chain = new IteratingFilterChain(filters.iterator(), new FilterChain()
        {
            public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException
            {
                fail("Exception should be thrown before reaching here.");
            }
        });
        try
        {
            chain.doFilter((HttpServletRequest) mockRequest.proxy(), (HttpServletResponse) mockResponse.proxy());
            fail("Exception should have been thrown");
        }
        catch (ServletException e)
        {
            // yay
        }
    }

    private Filter createClassLoaderCheckingFilter(final String name) throws URISyntaxException
    {
        final PluginClassLoader loader = new PluginClassLoader(new JarPluginArtifact(getFileForResource(FILTER_TEST_JAR)));
        Plugin plugin = new DefaultDynamicPlugin(null, loader);
        FilterAdapter testFilter = new FilterAdapter()
        {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
            {
                assertSame(name + " loader should be the thread context ClassLoader when entering", loader, Thread.currentThread().getContextClassLoader());
                chain.doFilter(request, response);
                assertSame(name + " loader should be the thread context ClassLoader when exiting", loader, Thread.currentThread().getContextClassLoader());
            }
        };

        ServletFilterModuleDescriptor filterDescriptor = new ServletFilterModuleDescriptorBuilder()
            .with(testFilter)
            .with(plugin)
            .build();
        
        final Filter delegatingFilter = new DelegatingPluginFilter(filterDescriptor);
        return delegatingFilter;
    }
    
    private Filter createExceptionThrowingFilter(final String name)
    {
        return new FilterAdapter()
        {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
            {
                throw new ServletException(name);
            }
        };
    }
}
