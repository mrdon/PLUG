package com.atlassian.plugin.servlet;

import javax.servlet.http.HttpServletRequest;

import junit.framework.TestCase;

import com.atlassian.plugin.servlet.descriptors.ServletModuleDescriptor;
import com.atlassian.plugin.servlet.descriptors.ServletModuleDescriptorBuilder;
import com.mockobjects.dynamic.Mock;

public class TestPluginHttpRequestWrapper extends TestCase
{
    Mock mockWrappedRequest;
    ServletModuleDescriptor descriptor;
    
    PluginHttpRequestWrapper request;
    
    public void setUp()
    {
        mockWrappedRequest = new Mock(HttpServletRequest.class);
        mockWrappedRequest.matchAndReturn("getServletPath", "/context/plugins");
        mockWrappedRequest.matchAndReturn("getPathInfo", "/plugin/servlet/path/to/resource");
        
        descriptor = new ServletModuleDescriptorBuilder().withPath("/plugin/servlet/*").build();
        
        request = new PluginHttpRequestWrapper((HttpServletRequest) mockWrappedRequest.proxy(), descriptor);
    }

    public void testGetPathInfo()
    {
        assertEquals("/path/to/resource", request.getPathInfo());
    }
    
    public void testGetServletPath()
    {
        assertEquals("/context/plugins/plugin/servlet", request.getServletPath());
    }
}
