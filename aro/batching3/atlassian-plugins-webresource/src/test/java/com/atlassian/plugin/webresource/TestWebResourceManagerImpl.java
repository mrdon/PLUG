package com.atlassian.plugin.webresource;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.impl.StaticPlugin;
import com.atlassian.plugin.servlet.AbstractFileServerServlet;
import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;
import junit.framework.TestCase;

public class TestWebResourceManagerImpl extends TestCase
{
    private Mock mockPluginAccessor;
    private WebResourceManager webResourceManager;

    private static final String ANIMAL_PLUGIN_VERSION = "2";
    private static final String BASEURL = "http://www.foo.com";
    private static final String SYSTEM_COUNTER = "123";
    private static final String SYSTEM_BUILD_NUMBER = "650";

    protected void setUp() throws Exception
    {
        super.setUp();

        mockPluginAccessor = new Mock(PluginAccessor.class);

        Mock webResourceIntegration = new Mock(WebResourceIntegration.class);
        webResourceIntegration.matchAndReturn("getPluginAccessor", mockPluginAccessor.proxy());

        PluginResourceLocator pluginResourceLocator = new PluginResourceLocatorImpl((WebResourceIntegration) webResourceIntegration.proxy(), null);
        webResourceManager = new WebResourceManagerImpl(pluginResourceLocator, (WebResourceIntegration) webResourceIntegration.proxy());

        webResourceIntegration.expectAndReturn("getBaseUrl", BASEURL);
        webResourceIntegration.expectAndReturn("getSystemBuildNumber", SYSTEM_BUILD_NUMBER);
        webResourceIntegration.expectAndReturn("getSystemCounter", SYSTEM_COUNTER);
    }

    protected void tearDown() throws Exception
    {
        webResourceManager = null;
        mockPluginAccessor = null;

        super.tearDown();
    }

    public void testGetStaticResourcePrefix()
    {
        String expectedPrefix = BASEURL + "/" + PluginResourceLocatorImpl.STATIC_RESOURCE_PREFIX + "/" +
            SYSTEM_BUILD_NUMBER + "/" + SYSTEM_COUNTER + "/" + PluginResourceLocatorImpl.STATIC_RESOURCE_SUFFIX;
        assertEquals(expectedPrefix, webResourceManager.getStaticResourcePrefix());
    }

    public void testGetStaticResourcePrefixWithCounter()
    {
        String resourceCounter = "456";
        String expectedPrefix = BASEURL + "/" + PluginResourceLocatorImpl.STATIC_RESOURCE_PREFIX + "/" +
            SYSTEM_BUILD_NUMBER + "/" + SYSTEM_COUNTER + "/" + resourceCounter + "/" + PluginResourceLocatorImpl.STATIC_RESOURCE_SUFFIX;
        assertEquals(expectedPrefix, webResourceManager.getStaticResourcePrefix(resourceCounter));
    }

    public void testGetStaticPluginResourcePrefix()
    {
        final String moduleKey = "confluence.extra.animal:animal";

        final Plugin animalPlugin = new StaticPlugin();
        animalPlugin.setKey("confluence.extra.animal");
        animalPlugin.setPluginsVersion(Integer.parseInt(ANIMAL_PLUGIN_VERSION));

        WebResourceModuleDescriptor moduleDescriptor = new WebResourceModuleDescriptor() {
            public String getCompleteKey()
            {
                return moduleKey;
            }

            public Plugin getPlugin()
            {
                return animalPlugin;
            }
        };

        mockPluginAccessor.expectAndReturn("getEnabledPluginModule", C.args(C.eq(moduleKey)), moduleDescriptor);

        String resourceName = "foo.js";
        String expectedPrefix = BASEURL + "/" + PluginResourceLocatorImpl.STATIC_RESOURCE_PREFIX + "/" + SYSTEM_BUILD_NUMBER +
            "/" + SYSTEM_COUNTER + "/" + ANIMAL_PLUGIN_VERSION + "/" + PluginResourceLocatorImpl.STATIC_RESOURCE_SUFFIX + "/" + AbstractFileServerServlet
            .SERVLET_PATH + "/" + AbstractFileServerServlet.RESOURCE_URL_PREFIX + "/" + moduleKey + "/" + resourceName;

        assertEquals(expectedPrefix, webResourceManager.getStaticPluginResource(moduleKey, resourceName));
    }
}