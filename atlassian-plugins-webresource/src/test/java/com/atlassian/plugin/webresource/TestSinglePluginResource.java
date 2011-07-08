package com.atlassian.plugin.webresource;

import junit.framework.TestCase;

public class TestSinglePluginResource extends TestCase
{
    public void testGetUrl() throws Exception
    {
        SinglePluginResource resource = new SinglePluginResource("foo.css", "test.plugin.key", false);
        assertEquals("/download/resources/test.plugin.key/foo.css", resource.getUrl());
    }
}
