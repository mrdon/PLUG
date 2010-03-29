package com.atlassian.plugin.servlet.filter;

import com.atlassian.plugin.servlet.ServletModuleManager;
import junit.framework.TestCase;
import org.mockito.Mock;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class TestServletFilterModuleContainerFilter extends TestCase
{
    @Mock private ServletModuleManager moduleManager;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    public void setUp() {
        initMocks(this);
    }

    public void testFilter() throws IOException, ServletException
    {
        when(moduleManager.getFilters(any(FilterLocation.class), eq("/myfilter"), any(FilterConfig.class), eq(FilterDispatcherCondition.REQUEST))).thenReturn(Collections.<Filter>emptyList());

        MyFilter filter = new MyFilter(moduleManager);

        when(request.getContextPath()).thenReturn("/myapp");
        when(request.getRequestURI()).thenReturn("/myapp/myfilter");

        filter.doFilter(request, response, filterChain);
        verify(filterChain).doFilter(any(ServletRequest.class), any(ServletResponse.class));
    }

    public void testFilterNoDispatcher() throws IOException, ServletException
    {
        when(moduleManager.getFilters(any(FilterLocation.class), eq("/myfilter"), any(FilterConfig.class), eq(FilterDispatcherCondition.REQUEST))).thenReturn(Collections.<Filter>emptyList());

        try
        {
            new MyFilterNoDispatcher(moduleManager);
            fail("Should have thrown exception on init due to lack of dispatcher value");
        }
        catch (ServletException ex)
        {
            // this is good
        }
    }

    public void testNoServletModuleManager() throws IOException, ServletException
    {
        MyFilter filter = new MyFilter(null);
        filter.doFilter(request, response, filterChain);
        verify(filterChain).doFilter(any(ServletRequest.class), any(ServletResponse.class));
    }

    static class MyFilter extends ServletFilterModuleContainerFilter
    {
        @Mock private FilterConfig filterConfig;

        private final ServletModuleManager moduleManager;

        public MyFilter(ServletModuleManager moduleManager) throws ServletException
        {
            initMocks(this);
            this.moduleManager = moduleManager;
            when(filterConfig.getInitParameter("location")).thenReturn("after-encoding");
            when(filterConfig.getInitParameter("dispatcher")).thenReturn("REQUEST");
            init(filterConfig);
        }

        protected ServletModuleManager getServletModuleManager()
        {
             return moduleManager;
        }
    }

    static class MyFilterNoDispatcher extends ServletFilterModuleContainerFilter
    {
        @Mock private FilterConfig filterConfig;

        private final ServletModuleManager moduleManager;

        public MyFilterNoDispatcher(ServletModuleManager moduleManager) throws ServletException
        {
            initMocks(this);
            this.moduleManager = moduleManager;
            when(filterConfig.getInitParameter("location")).thenReturn("after-encoding");
            init(filterConfig);
        }

        protected ServletModuleManager getServletModuleManager()
        {
             return moduleManager;
        }
    }
}
