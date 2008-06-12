package com.atlassian.plugin.osgi.loader.transform;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.net.URISyntaxException;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.Map;
import java.util.HashMap;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;

import com.atlassian.plugin.classloader.PluginClassLoader;
import com.atlassian.plugin.PluginParseException;
import org.osgi.framework.Constants;

public class TestDefaultPluginTransformer extends TestCase
{
    public void testGenerateManifest() throws URISyntaxException, IOException, PluginParseException
    {
        File file = new File(getClass().getResource("/myapp-1.0-plugin.jar").toURI());
        DefaultPluginTransformer transformer = new DefaultPluginTransformer();
        PluginClassLoader cl = new PluginClassLoader(file);
        byte[] manifest = transformer.generateManifest(cl, file);
        Manifest mf = new Manifest(new ByteArrayInputStream(manifest));
        Attributes attrs = mf.getMainAttributes();

        assertEquals("1.1", attrs.getValue(Constants.BUNDLE_VERSION));
        assertEquals("com.atlassian.plugin.sample", attrs.getValue(Constants.BUNDLE_SYMBOLICNAME));
        assertEquals("This is a brief textual description of the plugin", attrs.getValue(Constants.BUNDLE_DESCRIPTION));
        assertEquals("Atlassian Software Systems Pty Ltd", attrs.getValue(Constants.BUNDLE_VENDOR));
        assertEquals("http://www.atlassian.com", attrs.getValue(Constants.BUNDLE_DOCURL));
        assertEquals("com.mycompany.myapp", attrs.getValue(Constants.EXPORT_PACKAGE));
        
    }

    public void testAddFilesToZip() throws URISyntaxException, IOException
    {
        File file = new File(getClass().getResource("/myapp-1.0-plugin.jar").toURI());
        Map<String,byte[]> files = new HashMap<String, byte[]>() {{
            put("foo", "bar".getBytes());
        }};
        File copy = DefaultPluginTransformer.addFilesToExistingZip(file, files);
        assertNotNull(copy);
        assertTrue(!copy.getName().equals(file.getName()));
        assertTrue(copy.length() != file.length());

        ZipFile zip = new ZipFile(copy);
        ZipEntry entry = zip.getEntry("foo");
        assertNotNull(entry);
    }
}
