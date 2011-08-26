package com.atlassian.plugin.webresource;

import junit.framework.TestCase;

public class TestResourceUtils extends TestCase
{
    public void testGetType()
    {
        assertEquals("css", ResourceUtils.getType("/foo.css"));
        assertEquals("js", ResourceUtils.getType("/superbatch/js/foo.js"));
        assertEquals("", ResourceUtils.getType("/superbatch/js/foo."));
        assertEquals("", ResourceUtils.getType("/superbatch/js/foo"));
    }
}
