package com.atlassian.plugin.parsers;

import java.io.*;
import java.net.URL;
import java.util.Collections;

import junit.framework.TestCase;

import com.atlassian.plugin.*;
import com.atlassian.plugin.classloader.PluginClassLoader;
import com.atlassian.plugin.impl.DefaultDynamicPlugin;
import com.atlassian.plugin.util.ClassLoaderUtils;
import com.mockobjects.dynamic.Mock;

public class TestXmlDescriptorParser extends TestCase
{
    private static final String MISSING_INFO_TEST_FILE = "test-missing-plugin-info.xml";
    private static final String DUMMY_PLUGIN_FILE = "pooh-test-plugin.jar";

    public TestXmlDescriptorParser(String name)
    {
        super(name);
    }

    // CONF-12680 Test for missing plugin-info
    public void testMissingPluginInfo()
    {
        // mock up some supporting objects
        PluginClassLoader classLoader = new PluginClassLoader(new File(getTestFile("ap-plugins") + "/" + DUMMY_PLUGIN_FILE));
        Mock mockFactory = new Mock(ModuleDescriptorFactory.class);
        mockFactory.expect("getModuleDescriptorClass", "unknown-plugin");

        // create a Plugin for testing
        Plugin testPlugin = new DefaultDynamicPlugin((PluginArtifact) null, classLoader);

        try
        {
            XmlDescriptorParser parser = new XmlDescriptorParser(new FileInputStream(getTestFile(MISSING_INFO_TEST_FILE)));
            parser.configurePlugin((ModuleDescriptorFactory)mockFactory.proxy(), testPlugin);
            
            PluginInformation info = testPlugin.getPluginInformation();
            assertNotNull("Info should not be null", info);
        }
        catch (PluginParseException e)
        {
            fail("Plugin information parsing should not fail.");
        }
        catch (FileNotFoundException e)
        {
            // This shouldn't happen
            fail("Error setting up test");
        }
    }

    public void testPluginsApplicationVersionMinMax()
    {
        XmlDescriptorParser parser = parse(
                "<atlassian-plugin key='foo'>",
                "  <plugin-info>",
                "    <application-version min='3' max='4' />",
                "  </plugin-info>",
                "</atlassian-plugin>");
        assertEquals(3, (int)parser.getPluginInformation().getMinVersion());
        assertEquals(4, (int)parser.getPluginInformation().getMaxVersion());
    }

    public void testPluginsApplicationVersionMinMaxWithOnlyMin()
    {
        XmlDescriptorParser parser = parse(
                "<atlassian-plugin key='foo'>",
                "  <plugin-info>",
                "    <application-version min='3' />",
                "  </plugin-info>",
                "</atlassian-plugin>");
        assertEquals(3, (int)parser.getPluginInformation().getMinVersion());
        assertEquals(0, (int)parser.getPluginInformation().getMaxVersion());
    }

    public void testPluginsApplicationVersionMinMaxWithOnlyMax()
    {
        XmlDescriptorParser parser = parse(
                "<atlassian-plugin key='foo'>",
                "  <plugin-info>",
                "    <application-version max='3' />",
                "  </plugin-info>",
                "</atlassian-plugin>");
        assertEquals(3, (int)parser.getPluginInformation().getMaxVersion());
        assertEquals(0, (int)parser.getPluginInformation().getMinVersion());
    }

    // Also CONF-12680 test for missing "essential metadata"

    public void testPluginsVersion()
    {
        String xml = "<atlassian-plugin key=\"foo\" pluginsVersion=\"2\" />";
        XmlDescriptorParser parser = new XmlDescriptorParser(new ByteArrayInputStream(xml.getBytes()));
        assertEquals(2, parser.getPluginsVersion());
    }

    public void testPluginsVersionAfterConfigure()
    {
        XmlDescriptorParser parser = new XmlDescriptorParser(new ByteArrayInputStream("<atlassian-plugin key=\"foo\" plugins-version=\"2\" />".getBytes()));
        // mock up some supporting objects
        PluginClassLoader classLoader = new PluginClassLoader(new File(getTestFile("ap-plugins") + "/" + DUMMY_PLUGIN_FILE));
        Mock mockFactory = new Mock(ModuleDescriptorFactory.class);
        mockFactory.expect("getModuleDescriptorClass", "unknown-plugin");

        // create a Plugin for testing
        Plugin testPlugin = new DefaultDynamicPlugin((PluginArtifact) null, classLoader);
        parser.configurePlugin((ModuleDescriptorFactory)mockFactory.proxy(), testPlugin);
        assertEquals(2, testPlugin.getPluginsVersion());
    }

    public void testPluginsVersionWithDash()
    {
        String xml = "<atlassian-plugin key=\"foo\" plugins-version=\"2\" />";
        XmlDescriptorParser parser = new XmlDescriptorParser(new ByteArrayInputStream(xml.getBytes()));
        assertEquals(2, parser.getPluginsVersion());
    }

    public void testPluginsVersionMissing()
    {
        String xml = "<atlassian-plugin key=\"foo\" />";
        XmlDescriptorParser parser = new XmlDescriptorParser(new ByteArrayInputStream(xml.getBytes()));
        assertEquals(1, parser.getPluginsVersion());
    }
    

    private String getTestFile(String filename)
    {
        final URL url = ClassLoaderUtils.getResource(filename, this.getClass());
        return url.getFile();
    }

    private static XmlDescriptorParser parse(String... lines)
    {
        StringBuffer sb = new StringBuffer();
        for (String line : lines)
        {
            sb.append(line.replace('\'', '"')).append('\n');
        }
        InputStream in = new ByteArrayInputStream(sb.toString().getBytes());
        return new XmlDescriptorParser(in);
    }
}
