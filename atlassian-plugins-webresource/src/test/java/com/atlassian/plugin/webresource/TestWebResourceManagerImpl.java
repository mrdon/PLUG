package com.atlassian.plugin.webresource;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.elements.ResourceDescriptor;
import com.atlassian.plugin.impl.StaticPlugin;
import com.atlassian.plugin.servlet.AbstractFileServerServlet;
import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;
import junit.framework.TestCase;

import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.List;
import java.io.StringWriter;

public class TestWebResourceManagerImpl extends TestCase
{
    private Mock mockWebResourceIntegration;
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

        mockWebResourceIntegration = new Mock(WebResourceIntegration.class);
        mockWebResourceIntegration.matchAndReturn("getPluginAccessor", mockPluginAccessor.proxy());

        PluginResourceLocator pluginResourceLocator = new PluginResourceLocatorImpl((PluginAccessor) mockPluginAccessor.proxy(), null);
        webResourceManager = new WebResourceManagerImpl(pluginResourceLocator, (WebResourceIntegration) mockWebResourceIntegration.proxy());

        mockWebResourceIntegration.matchAndReturn("getBaseUrl", BASEURL);
        mockWebResourceIntegration.matchAndReturn("getSystemBuildNumber", SYSTEM_BUILD_NUMBER);
        mockWebResourceIntegration.matchAndReturn("getSystemCounter", SYSTEM_COUNTER);
    }

    protected void tearDown() throws Exception
    {
        webResourceManager = null;
        mockPluginAccessor = null;
        mockWebResourceIntegration = null;

        super.tearDown();
    }

    public void testRequireResources()
    {
        String resource1 = "test.atlassian:cool-stuff";
        String resource2 = "test.atlassian:hot-stuff";

        Map requestCache = new HashMap();
        mockWebResourceIntegration.matchAndReturn("getRequestCache", requestCache);
        webResourceManager.requireResource(resource1);
        webResourceManager.requireResource(resource2);
        webResourceManager.requireResource(resource1); // require again to test it only gets included once

        Collection resources = (Collection) requestCache.get(WebResourceManagerImpl.REQUEST_CACHE_RESOURCE_KEY);
        assertEquals(2, resources.size());
        assertTrue(resources.contains(resource1));
        assertTrue(resources.contains(resource2));
    }

    public void testRequireResourceAndResourceTagMethods() throws Exception
    {
        final String moduleKey = "cool-resources";
        final String completeModuleKey = "test.atlassian:" + moduleKey;

        final List<ResourceDescriptor> resourceDescriptors1 = TestUtils.createResourceDescriptors("cool.css", "more-cool.css", "cool.js");

        final int pluginVersion = 1;
        final Mock mockPlugin = new Mock(Plugin.class);
        mockPlugin.matchAndReturn("getPluginsVersion", pluginVersion);

        Map requestCache = new HashMap();
        mockWebResourceIntegration.matchAndReturn("getRequestCache", requestCache);

        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(completeModuleKey)), new WebResourceModuleDescriptor() {
            public String getCompleteKey()
            {
                return completeModuleKey;
            }

            public List getResourceDescriptors()
            {
                return resourceDescriptors1;
            }

            public Plugin getPlugin()
            {
                return (Plugin) mockPlugin.proxy();
            }
        });

        // test requireResource() methods
        StringWriter requiredResourceWriter = new StringWriter();
        webResourceManager.requireResource(completeModuleKey);
        webResourceManager.includeResources(requiredResourceWriter);
        String requiredResourceResult = webResourceManager.getRequiredResources();
        assertEquals(requiredResourceResult, requiredResourceWriter.toString());

        String staticBase = BASEURL + "/" + WebResourceManagerImpl.STATIC_RESOURCE_PREFIX  + "/" + SYSTEM_BUILD_NUMBER
            + "/" + SYSTEM_COUNTER + "/" + pluginVersion + "/" + WebResourceManagerImpl.STATIC_RESOURCE_SUFFIX + BatchPluginResource.URL_PREFIX;

        assertTrue(requiredResourceResult.contains("href=\"" + staticBase + "/css/" + completeModuleKey + ".css"));
        assertTrue(requiredResourceResult.contains("src=\"" + staticBase + "/js/" + completeModuleKey + ".js"));

        // test resourceTag() methods
        StringWriter resourceTagsWriter = new StringWriter();
        webResourceManager.requireResource(completeModuleKey, resourceTagsWriter);
        String resourceTagsResult = webResourceManager.getResourceTags(completeModuleKey);
        assertEquals(resourceTagsResult, resourceTagsWriter.toString());

        // calling requireResource() or resourceTag() on a single webresource should be the same
        assertEquals(requiredResourceResult, resourceTagsResult);
    }

    public void testGetStaticResourcePrefix()
    {
        String expectedPrefix = BASEURL + "/" + WebResourceManagerImpl.STATIC_RESOURCE_PREFIX + "/" +
            SYSTEM_BUILD_NUMBER + "/" + SYSTEM_COUNTER + "/" + WebResourceManagerImpl.STATIC_RESOURCE_SUFFIX;
        assertEquals(expectedPrefix, webResourceManager.getStaticResourcePrefix());
    }

    public void testGetStaticResourcePrefixWithCounter()
    {
        String resourceCounter = "456";
        String expectedPrefix = BASEURL + "/" + WebResourceManagerImpl.STATIC_RESOURCE_PREFIX + "/" +
            SYSTEM_BUILD_NUMBER + "/" + SYSTEM_COUNTER + "/" + resourceCounter + "/" + WebResourceManagerImpl.STATIC_RESOURCE_SUFFIX;
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
        String expectedPrefix = BASEURL + "/" + WebResourceManagerImpl.STATIC_RESOURCE_PREFIX + "/" + SYSTEM_BUILD_NUMBER +
            "/" + SYSTEM_COUNTER + "/" + ANIMAL_PLUGIN_VERSION + "/" + WebResourceManagerImpl.STATIC_RESOURCE_SUFFIX + "/" + AbstractFileServerServlet
            .SERVLET_PATH + "/" + AbstractFileServerServlet.RESOURCE_URL_PREFIX + "/" + moduleKey + "/" + resourceName;

        assertEquals(expectedPrefix, webResourceManager.getStaticPluginResource(moduleKey, resourceName));
    }
}
