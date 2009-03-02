package com.atlassian.plugin.osgi.container;

import junit.framework.TestCase;
import com.atlassian.plugin.test.PluginTestUtils;
import com.atlassian.plugin.osgi.container.impl.DefaultOsgiPersistentCache;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

public class TestDefaultOsgiPersistentCache extends TestCase
{
    private File tmpDir;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        tmpDir = PluginTestUtils.createTempDirectory(TestDefaultOsgiPersistentCache.class);
    }

    public void testRecordLastVersion() throws IOException
    {
        new DefaultOsgiPersistentCache(tmpDir, "1.0");
        File versionFile = new File(new File(tmpDir, "transformed-plugins"), "host.version");
        assertTrue(versionFile.exists());
        String txt = FileUtils.readFileToString(versionFile);
        assertEquals("1.0", txt);
    }

    public void testCleanOnUpgrade() throws IOException
    {
        new DefaultOsgiPersistentCache(tmpDir, "1.0");
        File tmp = File.createTempFile("foo", ".txt", new File(tmpDir, "transformed-plugins"));
        assertTrue(tmp.exists());
        new DefaultOsgiPersistentCache(tmpDir, "2.0");
        assertFalse(tmp.exists());
    }

    public void testNullVersion() throws IOException
    {
        new DefaultOsgiPersistentCache(tmpDir, null);
        File tmp = File.createTempFile("foo", ".txt", new File(tmpDir, "transformed-plugins"));
        assertTrue(tmp.exists());
        new DefaultOsgiPersistentCache(tmpDir, null);
        assertTrue(tmp.exists());
    }

    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();
        FileUtils.cleanDirectory(tmpDir);
    }
}
