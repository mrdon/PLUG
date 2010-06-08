package com.atlassian.plugin.servlet.util;

import junit.framework.TestCase;

public class TestPathMapper extends TestCase
{
    /**
     * For more info, see:
     * https://studio.atlassian.com/browse/APL-170
     * https://extranet.atlassian.com/display/~evzijst/2010/06/02/Bypassing+Servlet+Filters+with+Double+Slashes
     */
    public void testDoubleSlashes()
    {
        final PathMapper pathMapper = new DefaultPathMapper();

        pathMapper.put("key", "/foo/bar*");
        assertEquals("key", pathMapper.get("/foo/bar"));
        assertEquals("key", pathMapper.get("/foo//bar"));
        assertEquals("key", pathMapper.get("/foo///bar"));
    }

    public void testRemovePath()
    {
        final PathMapper pathMapper = new DefaultPathMapper();

        pathMapper.put("foo.bar", "/foo*");
        pathMapper.put("foo.baz", "/bar*");
        assertEquals("foo.bar", pathMapper.get("/foo/bar"));
        assertEquals("foo.baz", pathMapper.get("/bar/foo"));

        pathMapper.put("foo.bar", null);
        assertNull(pathMapper.get("/foo/bar"));
        assertEquals(0, pathMapper.getAll("/foo/bar").size());
        assertEquals("foo.baz", pathMapper.get("/bar/foo"));
    }
}
