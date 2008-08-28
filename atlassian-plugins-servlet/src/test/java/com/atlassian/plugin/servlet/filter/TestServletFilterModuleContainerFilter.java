package com.atlassian.plugin.servlet.filter;

import static com.mockobjects.dynamic.C.args;
import static com.mockobjects.dynamic.C.eq;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import com.atlassian.plugin.servlet.ServletModuleManager;
import com.atlassian.plugin.servlet.filter.FilterLocation;
import com.atlassian.plugin.servlet.filter.ServletFilterModuleContainerFilter;
import com.mockobjects.dynamic.Mock;

public class TestServletFilterModuleContainerFilter extends TestCase
{
    Mock mockServletModuleManager;
    
    ServletFilterModuleContainerFilter filter;
    
    public void setUp()
    {
        mockServletModuleManager = new Mock(ServletModuleManager.class);
        
        filter = new ServletFilterModuleContainerFilter()
        {
            @Override
            protected ServletModuleManager getServletModuleManager()
            {
                return (ServletModuleManager) mockServletModuleManager.proxy();
            }
        };
    }
    
    public void testFiltersCalledInProperOrder() throws IOException, ServletException
    {
        Mock mockRequest = new Mock(HttpServletRequest.class);
        mockRequest.expectAndReturn("getPathInfo", "some/path");
        Mock mockResponse = new Mock(HttpServletResponse.class);
        
        Mock mockFilterConfig = new Mock(FilterConfig.class);
        mockFilterConfig.expectAndReturn("getInitParameter", eq("location"), FilterLocation.top.toString()); 
        
        List<Integer> filterCallOrder = new LinkedList<Integer>();
        List<Filter> filters = new ArrayList<Filter>();
        for(int i = 0; i < 5; i++) {
            filters.add(new SoundOffFilter(filterCallOrder, i));
        }
        
        mockServletModuleManager.expectAndReturn(
            "getFilters", 
            args(eq(FilterLocation.top), eq("some/path"), eq(mockFilterConfig.proxy())),
            filters
        );
        
        filter.init((FilterConfig) mockFilterConfig.proxy());
        filter.doFilter((HttpServletRequest) mockRequest.proxy(), (HttpServletResponse) mockResponse.proxy(), singletonFilterChain(new SoundOffFilter(filterCallOrder, 100)));
        
        // make sure that all filters were called and unrolled in the proper order
        assertEquals(newList(0, 1, 2, 3, 4, 100, 100, 4, 3, 2, 1, 0), filterCallOrder);
    }
    
    <T> List<T> newList(T first, T... rest)
    {
        List<T> list = new LinkedList<T>();
        list.add(first);
        for (T element : rest)
        {
            list.add(element);
        }
        return list;
    }
    
    final static class SoundOffFilter implements Filter
    {
        private final List<Integer> filterCallOrder;
        private final int filterId;
        
        public SoundOffFilter(List<Integer> filterCallOrder, int filterId)
        {
            this.filterCallOrder = filterCallOrder;
            this.filterId = filterId;
        }

        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException
        {
            filterCallOrder.add(filterId);
            chain.doFilter(request, response);
            filterCallOrder.add(filterId);
        }
        
        public void init(FilterConfig arg0) throws ServletException {}
        public void destroy() {}
    }
    
    /**
     * Creates a filter chain from the single filter.  When this filter is called once, the filter chain is finished.
     */
    static FilterChain singletonFilterChain(final Filter filter)
    {
        return new FilterChain()
        {
            boolean called = false;
            
            public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException
            {
                if (!called)
                {
                    called = true;
                    filter.doFilter(request, response, this);
                }
            }
        };
    }
}
