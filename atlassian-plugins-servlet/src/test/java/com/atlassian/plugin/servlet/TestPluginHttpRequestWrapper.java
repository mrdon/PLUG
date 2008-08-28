package com.atlassian.plugin.servlet;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import junit.framework.TestCase;

import com.atlassian.plugin.servlet.PluginHttpRequestWrapper;
import com.atlassian.plugin.servlet.descriptors.MockServletModuleDescriptor;
import com.mockobjects.dynamic.Mock;

public class TestPluginHttpRequestWrapper extends TestCase
{
    Mock mockWrappedRequest;
    MockServletModuleDescriptor descriptor;
    
    PluginHttpRequestWrapper request;
    
    public void setUp()
    {
        mockWrappedRequest = new Mock(HttpServletRequest.class);
        mockWrappedRequest.matchAndReturn("getServletPath", "/context/plugins");
        mockWrappedRequest.matchAndReturn("getPathInfo", "/plugin/servlet/path/to/resource");
        
        List<String> paths = new ArrayList<String>();
        paths.add("/plugin/servlet/*");
        
        descriptor = new MockServletModuleDescriptor(null, null, paths);
        
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
