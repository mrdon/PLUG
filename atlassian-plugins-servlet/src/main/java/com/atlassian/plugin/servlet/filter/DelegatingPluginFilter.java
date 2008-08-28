package com.atlassian.plugin.servlet.filter;

import static com.atlassian.plugin.servlet.util.ClassLoaderSubstitutor.restoreThreadClassLoader;
import static com.atlassian.plugin.servlet.util.ClassLoaderSubstitutor.substituteThreadClassLoaderWithClassLoaderFrom;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import com.atlassian.plugin.servlet.PluginHttpRequestWrapper;
import com.atlassian.plugin.servlet.descriptors.ServletFilterModuleDescriptor;

/**
 * We wrap the plugins filter so that we can set some things up before the plugins filter is called. Currently we do 
 * the following:
 *      <ul>
 *        <li>set the Threads classloader to the plugins classloader)</li>
 *        <li>wrap the request so that path info is right for the filters</li>
 *      </ul>
 */
public class DelegatingPluginFilter implements Filter
{
    private final ServletFilterModuleDescriptor descriptor;
    private final Filter filter;
    
    public DelegatingPluginFilter(ServletFilterModuleDescriptor descriptor)
    {
        this.descriptor = descriptor;
        this.filter = descriptor.getModule();
    }

    public void init(FilterConfig filterConfig) throws ServletException
    {
        substituteThreadClassLoaderWithClassLoaderFrom(descriptor.getPlugin());
        filter.init(filterConfig);
        restoreThreadClassLoader();
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException
    {
        substituteThreadClassLoaderWithClassLoaderFrom(descriptor.getPlugin());
        filter.doFilter(new PluginHttpRequestWrapper((HttpServletRequest) request, descriptor), response, chain);
        restoreThreadClassLoader();
    }
    
    public void destroy()
    {
        substituteThreadClassLoaderWithClassLoaderFrom(descriptor.getPlugin());
        filter.destroy();
        restoreThreadClassLoader();
    }
}
