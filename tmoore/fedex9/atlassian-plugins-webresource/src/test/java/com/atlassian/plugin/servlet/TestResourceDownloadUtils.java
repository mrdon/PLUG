package com.atlassian.plugin.servlet;

import com.mockobjects.dynamic.Mock;
import com.mockobjects.dynamic.C;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

import junit.framework.TestCase;

public class TestResourceDownloadUtils extends TestCase
{
    private static final long TEN_YEARS = 1000L * 60L * 60L * 24L *365L * 10L;
    private static final String CACHE_CONTROL = "Cache-Control";

    public void testAddCachingHeaders()
    {
        Mock mockRequest = new Mock(HttpServletRequest.class);

        Mock mockResponse = new Mock(HttpServletResponse.class);
        mockResponse.expect("setDateHeader", C.ANY_ARGS);
        mockResponse.expect("setHeader", C.args(C.eq(CACHE_CONTROL), C.eq("max-age=" + TEN_YEARS)));
        mockResponse.expect("addHeader", C.args(C.eq(CACHE_CONTROL), C.eq("public")));

        ResourceDownloadUtils.addCachingHeaders((HttpServletRequest) mockRequest.proxy(), (HttpServletResponse) mockResponse.proxy());
    }

    public void testAddCachingHeadersWithCacheControls()
    {
        Mock mockResponse = new Mock(HttpServletResponse.class);
        mockResponse.expect("setDateHeader", C.ANY_ARGS);
        mockResponse.expect("setHeader", C.args(C.eq(CACHE_CONTROL), C.eq("max-age=" + TEN_YEARS)));
        mockResponse.expect("addHeader", C.args(C.eq(CACHE_CONTROL), C.eq("private")));
        mockResponse.expect("addHeader", C.args(C.eq(CACHE_CONTROL), C.eq("foo")));

        ResourceDownloadUtils.addCachingHeaders((HttpServletResponse) mockResponse.proxy(), "private", "foo");
    }
}
