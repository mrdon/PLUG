package com.atlassian.plugin.osgi.factory.transform.model;

import junit.framework.TestCase;

public class TestSystemExports extends TestCase
{
    public void testExportPackageWithVersion()
    {
        SystemExports exports = new SystemExports("foo.bar;version=\"4.0\"");

        assertEquals("foo.bar;version=\"[4.0,4.0]\"", exports.getFullExport("foo.bar"));
        assertEquals("foo.baz", exports.getFullExport("foo.baz"));
    }
}
