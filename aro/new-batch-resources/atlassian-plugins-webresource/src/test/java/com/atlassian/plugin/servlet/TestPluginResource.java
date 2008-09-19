package com.atlassian.plugin.servlet;

import junit.framework.TestCase;
import com.atlassian.plugin.webresource.PluginResource;

public class TestPluginResource extends TestCase
{
    public void testParseWithSimpleName()
    {
        PluginResource resource = PluginResource.parse("/download/resources/test.plugin.key:module/mydownload.jpg");
        assertEquals("test.plugin.key:module", resource.getModuleCompleteKey());
        assertEquals("mydownload.jpg", resource.getResourceName());
    }

    public void testParseWithSlashesInName()
    {
        PluginResource resource = PluginResource.parse("/download/resources/test.plugin.key:module/path/to/mydownload.jpg");
        assertEquals("test.plugin.key:module", resource.getModuleCompleteKey());
        assertEquals("path/to/mydownload.jpg", resource.getResourceName());
    }

    public void testGetUrl()
    {
        PluginResource resource = new PluginResource("test.plugin.key", "foo.css");
        assertEquals("/download/resources/test.plugin.key/foo.css", resource.getUrl());
    }

    public void testRoundTrip()
    {
        PluginResource resource = new PluginResource("test.plugin.key", "foo.css");
        String url = resource.getUrl();
        PluginResource parsedResource = PluginResource.parse(url);
        assertEquals(resource.getModuleCompleteKey(), parsedResource.getModuleCompleteKey());
        assertEquals(resource.getResourceName(), parsedResource.getResourceName());
    }
}
