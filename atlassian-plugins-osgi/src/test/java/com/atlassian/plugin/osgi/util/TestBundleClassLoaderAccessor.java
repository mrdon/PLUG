package com.atlassian.plugin.osgi.util;

import junit.framework.TestCase;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.osgi.framework.Bundle;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.Enumeration;

public class TestBundleClassLoaderAccessor extends TestCase
{
    public void testGetResource() throws IOException
    {
        Bundle bundle = mock(Bundle.class);
        when(bundle.getResource("foo.txt")).thenReturn(getClass().getClassLoader().getResource("foo.txt"));

        URL url = BundleClassLoaderAccessor.getClassLoader(bundle, null).getResource("foo.txt");
        assertNotNull(url);
    }

    public void testGetResourceAsStream() throws IOException
    {
        Bundle bundle = mock(Bundle.class);
        when(bundle.getResource("foo.txt")).thenReturn(getClass().getClassLoader().getResource("foo.txt"));

        InputStream in = BundleClassLoaderAccessor.getClassLoader(bundle, null).getResourceAsStream("foo.txt");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(in, out);
        assertTrue(out.toByteArray().length > 0);
    }

    public void testGetResources() throws IOException
    {
        Bundle bundle = mock(Bundle.class);
        when(bundle.getResources("foo.txt")).thenReturn(getClass().getClassLoader().getResources("foo.txt"));

        Enumeration<URL> e = BundleClassLoaderAccessor.getClassLoader(bundle, null).getResources("foo.txt");
        assertNotNull(e);
        assertTrue(e.hasMoreElements());
    }

    public void testGetResourcesIfNull() throws IOException
    {
        Bundle bundle = mock(Bundle.class);
        when(bundle.getResources("foo.txt")).thenReturn(null);

        Enumeration<URL> e = BundleClassLoaderAccessor.getClassLoader(bundle, null).getResources("foo.txt");
        assertNotNull(e);
        assertFalse(e.hasMoreElements());
    }
}
