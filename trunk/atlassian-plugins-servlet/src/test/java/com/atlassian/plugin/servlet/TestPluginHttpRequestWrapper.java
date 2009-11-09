package com.atlassian.plugin.servlet;

import javax.servlet.http.HttpServletRequest;

import junit.framework.TestCase;

import com.atlassian.plugin.servlet.descriptors.ServletModuleDescriptor;
import com.atlassian.plugin.servlet.descriptors.ServletModuleDescriptorBuilder;
import com.mockobjects.dynamic.Mock;

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
}
