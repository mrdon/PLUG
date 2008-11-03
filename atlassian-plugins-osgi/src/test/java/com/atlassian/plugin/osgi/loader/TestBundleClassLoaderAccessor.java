package com.atlassian.plugin.osgi.loader;

import junit.framework.TestCase;
import com.mockobjects.dynamic.Mock;
import com.mockobjects.dynamic.C;
import org.osgi.framework.Bundle;

import java.io.IOException;

public class TestBundleClassLoaderAccessor extends TestCase
{
    public void testGetResourceAsStream() throws IOException
    {
        Mock mockBundle = new Mock(Bundle.class);
        mockBundle.expectAndReturn("getResource", C.args(C.eq("/foo.txt")), getClass().getResource("/foo.txt"));

        BundleClassLoaderAccessor.getResourceAsStream((Bundle) mockBundle.proxy(), "/foo.txt");
        byte[] buffer = new byte[1024];
        assertTrue(buffer.length > 0);

    }
}