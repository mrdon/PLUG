package com.atlassian.plugin.servlet;

import junit.framework.TestCase;
import com.atlassian.plugin.PluginManager;
import com.atlassian.plugin.ModuleDescriptor;
import com.mockobjects.dynamic.Mock;
import com.mockobjects.dynamic.C;

public class TestPluginResourceDownload extends TestCase
{
    private PluginResourceDownload resourceDownload;

    protected void setUp() throws Exception
    {
        super.setUp();
        resourceDownload = new PluginResourceDownload();
    }

    public void testSplitLastPathPart()
    {
        final String[] parts = resourceDownload.splitLastPathPart(
            "http://localhost:8080/confluence/download/foo/bar/baz");
        assertEquals(2, parts.length);
        assertEquals("http://localhost:8080/confluence/download/foo/bar/", parts[0]);
        assertEquals("baz", parts[1]);

        final String[] anotherParts = resourceDownload.splitLastPathPart(parts[0]);
        assertEquals(2, anotherParts.length);
        assertEquals("http://localhost:8080/confluence/download/foo/", anotherParts[0]);
        assertEquals("bar/", anotherParts[1]);

        assertNull(resourceDownload.splitLastPathPart("noslashes"));
    }

    public void testGetDownloadablePluginModule()
    {
        Mock pluginManager = new Mock(PluginManager.class);
        Mock moduleDescriptor = new Mock(ModuleDescriptor.class);
        resourceDownload.setPluginManager((PluginManager) pluginManager.proxy());

        pluginManager.matchAndReturn("getPlugin", C.ANY_ARGS, null);
        moduleDescriptor.matchAndReturn("getPluginKey", null);
        moduleDescriptor.expectAndReturn("getResourceLocation", C.args(C.eq("download"), C.eq("foo/bar/baz/test.css")), null);
        moduleDescriptor.expectAndReturn("getResourceLocation", C.args(C.eq("download"), C.eq("foo/bar/baz/")), null);
        moduleDescriptor.expectAndReturn("getResourceLocation", C.args(C.eq("download"), C.eq("foo/bar/")), null);
        moduleDescriptor.expectAndReturn("getResourceLocation", C.args(C.eq("download"), C.eq("foo/")), null);

        resourceDownload.getResourceFromModule((ModuleDescriptor) moduleDescriptor.proxy(),
            "foo/bar/baz/test.css", "", new StubFileServerServlet(resourceDownload, "text/css"));

        moduleDescriptor.verify();
        pluginManager.verify();
    }
}
