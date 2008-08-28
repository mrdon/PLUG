package com.atlassian.plugin.servlet.filter;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.atlassian.plugin.servlet.ServletModuleManager;

public abstract class ServletFilterModuleContainerFilter implements Filter
{
    private static final Log log = LogFactory.getLog(ServletFilterModuleContainerFilter.class);
    
    private FilterConfig filterConfig;
    private FilterLocation location;

    public void init(FilterConfig filterConfig) throws ServletException
    {
        this.filterConfig = filterConfig;
        location = FilterLocation.valueOf(filterConfig.getInitParameter("location"));
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
    {
        doFilter((HttpServletRequest) request, (HttpServletResponse) response, chain);
    }
    
    void doFilter(HttpServletRequest request, HttpServletResponse response, final FilterChain chain) throws IOException, ServletException
    {
        if (getServletModuleManager() == null)
        {
            log.error("Could not get DefaultServletModuleManager?");
            response.sendError(500, "Could not get DefaultServletModuleManager.");
            return;
        }
        
        final List<Filter> filters = getServletModuleManager().getFilters(location, request.getPathInfo(), filterConfig);
        
        FilterChain pluginFilterChain = new FilterChain()
        {
            private Iterator<Filter> filterIt = filters.iterator();
            
            public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException
            {
                if (filterIt.hasNext())
                {
                    Filter filter = filterIt.next();
                    filter.doFilter(request, response, this);
                }
                else
                {
                    chain.doFilter(request, response);
                }
            }
        };
        pluginFilterChain.doFilter(request, response);
    }
    
    public void destroy()
    {
    }

    /**
     * Retrieve the DefaultServletModuleManager from your container framework.
     */
    protected abstract ServletModuleManager getServletModuleManager();
}
