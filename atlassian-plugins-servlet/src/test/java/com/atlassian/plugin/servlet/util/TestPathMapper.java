package com.atlassian.plugin.servlet.util;

import com.atlassian.plugin.servlet.util.DefaultPathMapper;
import com.atlassian.plugin.servlet.util.PathMapper;

import junit.framework.TestCase;

public class TestPathMapper extends TestCase
{
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
