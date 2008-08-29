package com.atlassian.plugin.servlet.filter;

import java.io.IOException;
import java.util.Iterator;

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

/**
 * Applications need to create a concrete subclass of this for use in their filter stack.  This filters responsiblity
 * is to retrieve the filters to be applied to the request from the {@link ServletModuleManager} and build a
 * {@link FilterChain} from them.  Once all the filters in the chain have been applied to the request, this filter 
 * returns control to the main chain.
 * <p/>
 * There is one init parameters that must be configured for this filter, the "location" parameter.  It can be one of
 * "top", "middle" or "bottom".  A filter with a "top" location must appear before the filter with a "middle" location
 * which must appear before a filter with a "bottom" location.  Where any other application filters lie in the filter
 * stack is completely up to the application.        
 */
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
        
        final Iterable<Filter> filters = getServletModuleManager().getFilters(location, request.getPathInfo(), filterConfig);
        
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
