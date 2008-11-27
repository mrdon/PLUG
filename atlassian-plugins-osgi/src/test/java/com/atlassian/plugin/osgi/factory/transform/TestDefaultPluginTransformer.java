package com.atlassian.plugin.osgi.factory.transform;

import com.atlassian.plugin.PluginManager;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.test.PluginJarBuilder;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;
import com.atlassian.plugin.osgi.hostcomponents.impl.MockRegistration;
import com.atlassian.plugin.osgi.factory.transform.test.SomeClass;
import com.atlassian.plugin.test.PluginTestUtils;
import junit.framework.TestCase;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.Filter;
import org.dom4j.DocumentException;
import org.osgi.framework.Constants;

import javax.print.attribute.AttributeSet;
import javax.print.attribute.HashAttributeSet;
import javax.swing.table.TableModel;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class TestDefaultPluginTransformer extends TestCase
{

    public void testAddFilesToZip() throws URISyntaxException, IOException
    {
        final File file = PluginTestUtils.getFileForResource("myapp-1.0-plugin.jar");

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

    public void testTransform() throws Exception
    {
        final File file = new PluginJarBuilder()
            .addFormattedJava("my.Foo", "package my;",
                                        "public class Foo {",
                                        "  com.atlassian.plugin.osgi.factory.transform.Fooable bar;",
                                        "}")
            .addPluginInformation("foo", "foo", "1.1")
            .build();


        DefaultPluginTransformer transformer = new DefaultPluginTransformer(PluginManager.PLUGIN_DESCRIPTOR_FILENAME);
        File copy = transformer.transform(file, new ArrayList<HostComponentRegistration>() {{
            add(new StubHostComponentRegistration(Fooable.class));
        }});

        assertNotNull(copy);
        final JarFile jar = new JarFile(copy);
        final Attributes attrs = jar.getManifest().getMainAttributes();

        assertEquals("1.1", attrs.getValue(Constants.BUNDLE_VERSION));

        assertNotNull(jar.getEntry("META-INF/spring/atlassian-plugins-host-components.xml"));
    }

}
