package com.atlassian.plugin.servlet.filter;

import junit.framework.TestCase;
import com.atlassian.plugin.servlet.ServletModuleManager;
import com.mockobjects.dynamic.Mock;
import com.mockobjects.dynamic.C;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.FilterConfig;
import java.io.IOException;
import java.util.Collections;

public class TestServletFilterModuleContainerFilter extends TestCase
{
    public void testFilter() throws IOException, ServletException
    {
        Mock mockManager = new Mock(ServletModuleManager.class);
        mockManager.expectAndReturn("getFilters", C.args(C.IS_ANYTHING, C.eq("/myfilter"), C.IS_ANYTHING), Collections.emptyList());

        MyFilter filter = new MyFilter(mockManager);
        Mock mockRequest = new Mock(HttpServletRequest.class);
        mockRequest.expectAndReturn("getContextPath", "/myapp");
        mockRequest.expectAndReturn("getContextPath", "/myapp");
        mockRequest.expectAndReturn("getRequestURI", "/myapp/myfilter");
        mockRequest.matchAndReturn("getAttribute", C.ANY_ARGS, null);
        mockRequest.expectAndReturn("getServletPath", null);
        mockRequest.expectAndReturn("getPathInfo", null);

        Mock mockResponse = new Mock(HttpServletResponse.class);
        Mock mockChain = new Mock(FilterChain.class);
        mockChain.expect("doFilter", C.ANY_ARGS);

        filter.doFilter((HttpServletRequest)mockRequest.proxy(), (HttpServletResponse)mockResponse.proxy(), (FilterChain)mockChain.proxy());

        mockRequest.verify();
        mockResponse.verify();
        mockChain.verify();
        mockManager.verify();
    }

    static class MyFilter extends ServletFilterModuleContainerFilter
    {
        private final Mock mockManager;

        public MyFilter(Mock mockManager) throws ServletException
        {
            this.mockManager = mockManager;
            Mock config = new Mock(FilterConfig.class);
            config.expectAndReturn("getInitParameter", C.ANY_ARGS, "after-encoding");
            init((FilterConfig) config.proxy());
        }

        protected ServletModuleManager getServletModuleManager()
        {
            return (ServletModuleManager) mockManager.proxy();
        }
    }
}
