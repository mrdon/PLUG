package com.atlassian.plugin.servlet;

import com.mockobjects.constraint.Constraint;
import com.mockobjects.dynamic.Mock;
import com.mockobjects.dynamic.C;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

import junit.framework.TestCase;

import java.util.Date;

public class TestResourceDownloadUtils extends TestCase
{
    private static final long ONE_YEAR = 60L * 60L * 24L *365L;
    private static final long ONE_YEAR_MS = ONE_YEAR * 1000;
    private static final String CACHE_CONTROL = "Cache-Control";

    public void testAddPublicCachingHeaders()
    {
        Mock mockRequest = new Mock(HttpServletRequest.class);

        Mock mockResponse = new Mock(HttpServletResponse.class);
        mockResponse.expect("setDateHeader", C.args(C.eq("Expires"), aboutNowPlusAYear()));
        mockResponse.expect("setHeader", C.args(C.eq(CACHE_CONTROL), C.eq("max-age=" + ONE_YEAR)));
        mockResponse.expect("addHeader", C.args(C.eq(CACHE_CONTROL), C.eq("public")));

        ResourceDownloadUtils.addPublicCachingHeaders((HttpServletRequest) mockRequest.proxy(), (HttpServletResponse) mockResponse.proxy());
        mockResponse.verify();
    }

    public void testAddCachingHeadersWithCacheControls()
    {
        Mock mockResponse = new Mock(HttpServletResponse.class);
        mockResponse.expect("setDateHeader", C.args(C.eq("Expires"), aboutNowPlusAYear()));
        mockResponse.expect("setHeader", C.args(C.eq(CACHE_CONTROL), C.eq("max-age=" + ONE_YEAR)));
        mockResponse.expect("addHeader", C.args(C.eq(CACHE_CONTROL), C.eq("private")));
        mockResponse.expect("addHeader", C.args(C.eq(CACHE_CONTROL), C.eq("foo")));

        ResourceDownloadUtils.addCachingHeaders((HttpServletResponse) mockResponse.proxy(), "private", "foo");
        mockResponse.verify();
    }

    private static Constraint aboutNowPlusAYear()
    {
        return new Constraint()
        {
            private final long nowPlusAYear = System.currentTimeMillis() + ONE_YEAR_MS;
            private final long nowPlusAYearPlus1s = nowPlusAYear + 1000;
            public boolean eval(Object o)
            {
                long time = (Long) o;
                return time >= nowPlusAYear && time < nowPlusAYearPlus1s;
            }

            @Override
            public String toString()
            {
                return "time >= " + nowPlusAYear + " and < " + nowPlusAYearPlus1s;
            }
        };
    }
}
