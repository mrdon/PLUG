package com.atlassian.plugin.webresource;

import com.atlassian.plugin.Plugin;
import com.mockobjects.dynamic.Mock;
import junit.framework.TestCase;
import org.dom4j.DocumentHelper;

import java.util.List;

public class TestWebResourceModuleDescriptor extends TestCase
{
    private static final String TEST_PLUGIN_KEY = "atlassian.test.plugin";

    private WebResourceModuleDescriptor descriptor;
    private Mock mockPlugin;

    protected void setUp() throws Exception
    {
        super.setUp();

        descriptor = new WebResourceModuleDescriptor();
        mockPlugin = new Mock(Plugin.class);
        mockPlugin.matchAndReturn("getKey", TEST_PLUGIN_KEY);
    }

    protected void tearDown() throws Exception
    {
        descriptor = null;
        mockPlugin = null;

        super.tearDown();
    }

    public void testInitWithDependencies() throws Exception
    {
        String xml = "<web-resource key=\"test-resources\">\n" +
                        "<dependency>jquery</dependency>\n" +
                        "<dependency>ajs</dependency>\n" +
                    "</web-resource>";

        descriptor.init((Plugin) mockPlugin.proxy(), DocumentHelper.parseText(xml).getRootElement());

        List<String> dependencies = descriptor.getDependencies();
        assertEquals(2, dependencies.size());
        assertEquals(TEST_PLUGIN_KEY + ":jquery", dependencies.get(0));
        assertEquals(TEST_PLUGIN_KEY + ":ajs", dependencies.get(1));
    }

    public void testInitWithOtherPluginDependencies() throws Exception
    {
        String xml = "<web-resource key=\"test-resources\">\n" +
                        "<dependency>jquery</dependency>\n" +
                        "<dependency plugin=\"atlassian.confluence.foo\">bar</dependency>\n" +
                    "</web-resource>";

        descriptor.init((Plugin) mockPlugin.proxy(), DocumentHelper.parseText(xml).getRootElement());

        List<String> dependencies = descriptor.getDependencies();
        assertEquals(2, dependencies.size());
        assertEquals(TEST_PLUGIN_KEY + ":jquery", dependencies.get(0));
        assertEquals("atlassian.confluence.foo:bar", dependencies.get(1));
    }
}
