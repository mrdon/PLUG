package com.atlassian.plugin.osgi.factory.transform;

import com.atlassian.plugin.osgi.factory.transform.JarUtils.Extractor;
import com.atlassian.plugin.test.PluginJarBuilder;

import com.google.common.collect.Iterables;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import junit.framework.TestCase;

public class TestJarUtils extends TestCase
{
    public void testEntries() throws Exception
    {
        final File plugin = new PluginJarBuilder().addResource("foo", "bar").build();
        final Iterable<JarEntry> entries = JarUtils.getEntries(plugin);
        assertFalse("must contain some entry", Iterables.isEmpty(entries));
        final JarEntry entry = Iterables.get(entries, 0);
        assertEquals("foo", entry.getName());
    }

    public void testNoEntries() throws Exception
    {
        // cannot create a completely empty jar so we just use the manifest
        final File plugin = new PluginJarBuilder().build();
        final Iterable<JarEntry> entries = JarUtils.getEntries(plugin);
        final JarEntry manifest = Iterables.getOnlyElement(entries);
        assertNotNull(manifest);
        assertEquals("META-INF/MANIFEST.MF", manifest.getName());
    }

    public void testEntry() throws Exception
    {
        final File plugin = new PluginJarBuilder().addResource("foo", "bar").build();
        final JarEntry entry = JarUtils.getEntry(plugin, "foo");
        assertNotNull(entry);
        assertEquals("foo", entry.getName());
    }

    public void testEntryNotFound() throws Exception
    {
        final File plugin = new PluginJarBuilder().addResource("foo", "bar").build();
        final JarEntry entry = JarUtils.getEntry(plugin, "bar");
        assertNull(entry);
    }

    public void testManifest() throws Exception
    {
        final File plugin = new PluginJarBuilder().build();
        final Manifest manifest = JarUtils.getManifest(plugin);
        assertNotNull(manifest);
    }

    public void testManifestCreatedIfNotPresent() throws Exception
    {
        final File plugin = new PluginJarBuilder().addResource("foo", "bar").buildWithNoManifest();
        final Manifest manifest = JarUtils.getManifest(plugin);
        assertNotNull(manifest);
    }

    public void testExtractor() throws Exception
    {
        final File plugin = new PluginJarBuilder().addResource("dooby", "whacker").build();
        final Object expected = new Object();
        final AtomicBoolean called = new AtomicBoolean(false);
        final Object result = JarUtils.withJar(plugin, new Extractor<Object>()
        {
            public Object get(final JarFile jar)
            {
                final JarEntry entry = jar.getJarEntry("dooby");
                assertNotNull(entry);
                final JarEntry nonEntry = jar.getJarEntry("blot");
                assertNull(nonEntry);
                called.set(true);
                return expected;
            }
        });
        assertTrue(called.get());
        assertSame(expected, result);
    }
}
