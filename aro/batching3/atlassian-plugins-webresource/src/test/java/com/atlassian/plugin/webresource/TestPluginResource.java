package com.atlassian.plugin.webresource;

import junit.framework.TestCase;

public class TestPluginResource extends TestCase
{
    public void testParseWithSimpleName()
    {
        SinglePluginResource resource = SinglePluginResource.parse("/download/resources/test.plugin.key:module/mydownload.jpg");
        assertEquals("test.plugin.key:module", resource.getModuleCompleteKey());
        assertEquals("mydownload.jpg", resource.getResourceName());
    }

    public void testParseWithSlashesInName()
    {
        SinglePluginResource resource = SinglePluginResource.parse("/download/resources/test.plugin.key:module/path/to/mydownload.jpg");
        assertEquals("test.plugin.key:module", resource.getModuleCompleteKey());
        assertEquals("path/to/mydownload.jpg", resource.getResourceName());
    }

    public void testGetUrl()
    {
        SinglePluginResource resource = new SinglePluginResource("foo.css", "test.plugin.key", "");
        assertEquals("/download/resources/test.plugin.key/foo.css", resource.getUrl());
    }

    public void testRoundTrip()
    {
        SinglePluginResource resource = new SinglePluginResource("foo.css", "test.plugin.key", "");
        String url = resource.getUrl();
        SinglePluginResource parsedResource = SinglePluginResource.parse(url);
        assertEquals(resource.getModuleCompleteKey(), parsedResource.getModuleCompleteKey());
        assertEquals(resource.getResourceName(), parsedResource.getResourceName());
    }
}