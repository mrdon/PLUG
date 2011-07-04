package com.atlassian.plugin.webresource;

import junit.framework.TestCase;

public class TestSuperBatchPluginResource extends TestCase
{
    public void testGetType()
    {
        assertEquals("css", SuperBatchPluginResource.getType("/foo.css"));
        assertEquals("js", SuperBatchPluginResource.getType("/superbatch/js/foo.js"));
        assertEquals("", SuperBatchPluginResource.getType("/superbatch/js/foo."));
        assertEquals("", SuperBatchPluginResource.getType("/superbatch/js/foo"));
    }
}
