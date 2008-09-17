package com.atlassian.plugin.parsers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URL;

import junit.framework.TestCase;

import org.mockito.Mockito;

import com.atlassian.plugin.ModuleDescriptorFactory;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.classloader.PluginClassLoader;
import com.atlassian.plugin.impl.DefaultDynamicPlugin;
import com.atlassian.plugin.util.ClassLoaderUtils;

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
        ModuleDescriptorFactory mockFactory = Mockito.mock(ModuleDescriptorFactory.class);

        // create a Plugin for testing
        Plugin testPlugin = new DefaultDynamicPlugin(null, classLoader);

        try
        {
            XmlDescriptorParser parser = new XmlDescriptorParser(new FileInputStream(getTestFile(MISSING_INFO_TEST_FILE)));
            parser.configurePlugin(mockFactory, testPlugin);
        }
        catch (PluginParseException e)
        {
            return;
        }
        catch (FileNotFoundException e)
        {
            // This shouldn't happen
            e.printStackTrace();
        }

        fail("Wrong/No exception thrown");
    }

    // Also CONF-12680 test for missing "essential metadata"
    

    private String getTestFile(String filename)
    {
        final URL url = ClassLoaderUtils.getResource(filename, this.getClass());
        return url.getFile();
    }
}
