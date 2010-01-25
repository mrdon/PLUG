package com.atlassian.plugin.servlet;

import com.atlassian.plugin.servlet.descriptors.ServletModuleDescriptor;
import com.atlassian.plugin.servlet.descriptors.ServletModuleDescriptorBuilder;
import com.mockobjects.dynamic.Mock;
import junit.framework.TestCase;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class TestPluginHttpRequestWrapper extends TestCase
{
    public void testWildcardMatching()
    {
        PluginHttpRequestWrapper request = getWrappedRequest("/context/plugins", "/plugin/servlet/path/to/resource",
            new ServletModuleDescriptorBuilder().withPath("/plugin/servlet/*").build());

        assertEquals("/path/to/resource", request.getPathInfo());
        assertEquals("/context/plugins/plugin/servlet", request.getServletPath());
    }

    public void testExactPathMatching()
    {
        PluginHttpRequestWrapper request = getWrappedRequest("/context/plugins", "/plugin/servlet",
            new ServletModuleDescriptorBuilder().withPath("/plugin/servlet").build());

        assertNull(request.getPathInfo());
        assertEquals("/context/plugins/plugin/servlet", request.getServletPath());
    }

    private PluginHttpRequestWrapper getWrappedRequest(String servletPath, String pathInfo,
        ServletModuleDescriptor servletModuleDescriptor)
    {
        Mock mockWrappedRequest = new Mock(HttpServletRequest.class);
        mockWrappedRequest.matchAndReturn("getServletPath", servletPath);
        mockWrappedRequest.matchAndReturn("getPathInfo", pathInfo);
        return new PluginHttpRequestWrapper((HttpServletRequest) mockWrappedRequest.proxy(), servletModuleDescriptor);
    }

    public void testGetSessionFalse() throws Exception
    {
        Mock mockWrappedRequest = new Mock(HttpServletRequest.class);
        mockWrappedRequest.matchAndReturn("getPathInfo", null);
        // delegate will have getSession(false) called and return null.
        mockWrappedRequest.matchAndReturn("getSession", false, null);
        PluginHttpRequestWrapper request = new PluginHttpRequestWrapper((HttpServletRequest) mockWrappedRequest.proxy(), null);

        assertNull(request.getSession(false));
    }

    public void testGetSession() throws Exception
    {
        // Mock the Session
        Mock mockSession = new Mock(HttpSession.class);
        mockSession.matchAndReturn("getAttribute", "foo", "bar");
        HttpSession realSession = (HttpSession) mockSession.proxy();

        // Mock the Request
        Mock mockWrappedRequest = new Mock(HttpServletRequest.class);
        // getPathInfo(0 gets called in constructor
        mockWrappedRequest.matchAndReturn("getPathInfo", null);
        // delegate will have getSession(true) called and return null.
        mockWrappedRequest.matchAndReturn("getSession", true, realSession);
        PluginHttpRequestWrapper request = new PluginHttpRequestWrapper((HttpServletRequest) mockWrappedRequest.proxy(), null);

        HttpSession wrappedSession = request.getSession();
        assertTrue(wrappedSession instanceof PluginHttpSessionWrapper);
        assertEquals("bar", wrappedSession.getAttribute("foo"));
    }

    public void testGetSessionTrue() throws Exception
    {
        // Mock the Session
        Mock mockSession = new Mock(HttpSession.class);
        mockSession.matchAndReturn("getAttribute", "foo", "bar");
        HttpSession realSession = (HttpSession) mockSession.proxy();

        // Mock the Request
        Mock mockWrappedRequest = new Mock(HttpServletRequest.class);
        // getPathInfo(0 gets called in constructor
        mockWrappedRequest.matchAndReturn("getPathInfo", null);
        // delegate will have getSession(true) called and return null.
        mockWrappedRequest.matchAndReturn("getSession", true, realSession);
        PluginHttpRequestWrapper request = new PluginHttpRequestWrapper((HttpServletRequest) mockWrappedRequest.proxy(), null);

        HttpSession wrappedSession = request.getSession(true);
        assertTrue(wrappedSession instanceof PluginHttpSessionWrapper);
        assertEquals("bar", wrappedSession.getAttribute("foo"));
    }

}
