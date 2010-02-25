package com.atlassian.plugin.webresource;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.module.ModuleClassFactory;
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
        Mock mockModuleCreator = new Mock(ModuleClassFactory.class);

        descriptor = new WebResourceModuleDescriptor((ModuleClassFactory) mockModuleCreator.proxy());
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
                        "<dependency>atlassian.test.plugin:jquery</dependency>\n" +
                        "<dependency>atlassian.test.plugin:ajs</dependency>\n" +
                    "</web-resource>";

        descriptor.init((Plugin) mockPlugin.proxy(), DocumentHelper.parseText(xml).getRootElement());

        List<String> dependencies = descriptor.getDependencies();
        assertEquals(2, dependencies.size());
        assertEquals("atlassian.test.plugin:jquery", dependencies.get(0));
        assertEquals("atlassian.test.plugin:ajs", dependencies.get(1));
    }
}
