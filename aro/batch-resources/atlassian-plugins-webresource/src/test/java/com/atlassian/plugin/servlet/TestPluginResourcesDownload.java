package com.atlassian.plugin.servlet;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.webresource.WebResourceModuleDescriptor;
import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;
import junit.framework.TestCase;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.List;

public class TestPluginResourcesDownload extends TestCase
{
    private PluginResourcesDownload resourcesDownload;

    protected void setUp() throws Exception
    {
        super.setUp();
        resourcesDownload = new PluginResourcesDownload();
    }

    protected void tearDown() throws Exception
    {
        resourcesDownload = null;
        super.tearDown();
    }

    public void testGetPluginResourcesFromModule() throws Exception
    {
        String completeKey = "test.plugin:test-resources";

        resourcesDownload.setSupportedExtension("css");

        Mock mockContentTypeResolver = new Mock(ContentTypeResolver.class);
        resourcesDownload.setContentTypeResolver((ContentTypeResolver) mockContentTypeResolver.proxy());

        Mock mockPluginAccessor = new Mock(PluginAccessor.class);
        resourcesDownload.setPluginAccessor((PluginAccessor) mockPluginAccessor.proxy());

        WebResourceModuleDescriptor moduleDescriptor = new WebResourceModuleDescriptor() {
            public List getResourceDescriptors()
            {
                return Collections.EMPTY_LIST;
            }
        };
        mockPluginAccessor.expectAndReturn("getPluginModule", C.args(C.eq(completeKey)), moduleDescriptor);
        mockPluginAccessor.expectAndReturn("isPluginModuleEnabled", C.args(C.eq(completeKey)), true);

        Mock mockRequest = new Mock(HttpServletRequest.class);
        mockRequest.expectAndReturn("getRequestURI", "/download/resources/css/" + completeKey);

        Mock mockResponse = new Mock(HttpServletResponse.class);

        resourcesDownload.serveFile((HttpServletRequest)mockRequest.proxy(), (HttpServletResponse)mockResponse.proxy());

        mockRequest.verify();
        mockResponse.verify();
        mockPluginAccessor.verify();
    }

    public void testGetPluginResourcesFromPlugin() throws Exception
    {
        String pluginKey = "test.plugin";

        resourcesDownload.setSupportedExtension("js");

        Mock mockContentTypeResolver = new Mock(ContentTypeResolver.class);
        resourcesDownload.setContentTypeResolver((ContentTypeResolver) mockContentTypeResolver.proxy());

        Mock mockPluginAccessor = new Mock(PluginAccessor.class);
        resourcesDownload.setPluginAccessor((PluginAccessor) mockPluginAccessor.proxy());

        Mock mockPlugin = new Mock(Plugin.class);
        mockPlugin.expectAndReturn("getResourceDescriptors", C.args(C.eq("download")), Collections.EMPTY_LIST);

        mockPluginAccessor.expectAndReturn("getPlugin", C.args(C.eq(pluginKey)), mockPlugin.proxy());
        mockPlugin.expectAndReturn("isEnabled", true);

        Mock mockRequest = new Mock(HttpServletRequest.class);
        mockRequest.expectAndReturn("getRequestURI", "/download/resources/js/" + pluginKey);

        Mock mockResponse = new Mock(HttpServletResponse.class);

        resourcesDownload.serveFile((HttpServletRequest)mockRequest.proxy(), (HttpServletResponse)mockResponse.proxy());

        mockRequest.verify();
        mockResponse.verify();
        mockPluginAccessor.verify();
    }
}
