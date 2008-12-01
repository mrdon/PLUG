package com.atlassian.plugin.webresource;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.elements.ResourceDescriptor;
import com.atlassian.plugin.elements.ResourceLocation;
import com.atlassian.plugin.servlet.DownloadableClasspathResource;
import com.atlassian.plugin.servlet.DownloadableResource;
import com.atlassian.plugin.servlet.ServletContextFactory;
import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;
import junit.framework.TestCase;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class TestPluginResourceLocatorImpl extends TestCase
{
    private PluginResourceLocatorImpl pluginResourceLocator;
    private Mock mockPluginAccessor;
    private Mock mockServletContextFactory;
    private static final String TEST_PLUGIN_KEY = "test.plugin";
    private static final String TEST_MODULE_KEY = "web-resources";
    private static final String TEST_MODULE_COMPLETE_KEY = TEST_PLUGIN_KEY + ":" + TEST_MODULE_KEY;

    protected void setUp() throws Exception
    {
        super.setUp();

        mockPluginAccessor = new Mock(PluginAccessor.class);
        mockServletContextFactory = new Mock(ServletContextFactory.class);


        pluginResourceLocator = new PluginResourceLocatorImpl(
            (PluginAccessor) mockPluginAccessor.proxy(),
            (ServletContextFactory) mockServletContextFactory.proxy()
        );
    }

    protected void tearDown() throws Exception
    {
        pluginResourceLocator = null;
        mockPluginAccessor = null;
        mockServletContextFactory = null;

        super.tearDown();
    }

    public void testGetAndParseUrl()
    {
        String url = pluginResourceLocator.getResourceUrl("plugin.key:my-resources", "foo.css");
        assertTrue(pluginResourceLocator.matches(url));
    }

    public void testGetPluginResourcesWithoutBatching() throws Exception
    {
        final Mock mockPlugin = new Mock(Plugin.class);
        mockPlugin.matchAndReturn("getPluginsVersion", 1);

        final List<ResourceDescriptor> resourceDescriptors = TestUtils.createResourceDescriptors("master-ie.css", "master.css", "comments.css");

        WebResourceModuleDescriptor moduleDescriptor = new WebResourceModuleDescriptor() {
            public String getCompleteKey()
            {
                return TEST_MODULE_COMPLETE_KEY;
            }

            public List getResourceDescriptors()
            {
                return resourceDescriptors;
            }

            public Plugin getPlugin()
            {
                return (Plugin) mockPlugin.proxy();
            }
        };
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(TEST_MODULE_COMPLETE_KEY)), moduleDescriptor);

        System.setProperty(PluginResourceLocatorImpl.PLUGIN_WEBRESOURCE_BATCHING_OFF, "true");
        try
        {
            List<PluginResource> resources = pluginResourceLocator.getPluginResources(TEST_MODULE_COMPLETE_KEY);
            assertEquals(3, resources.size());
        }
        finally
        {
            System.setProperty(PluginResourceLocatorImpl.PLUGIN_WEBRESOURCE_BATCHING_OFF, "false");
        }
    }

    public void testGetPluginResourcesWithBatching() throws Exception
    {
        final Mock mockPlugin = new Mock(Plugin.class);
        mockPlugin.matchAndReturn("getPluginsVersion", 1);

        final List<ResourceDescriptor> resourceDescriptors = TestUtils.createResourceDescriptors("master-ie.css", "master.css", "comments.css");

        WebResourceModuleDescriptor moduleDescriptor = new WebResourceModuleDescriptor() {
            public String getCompleteKey()
            {
                return TEST_MODULE_COMPLETE_KEY;
            }

            public List getResourceDescriptors()
            {
                return resourceDescriptors;
            }

            public Plugin getPlugin()
            {
                return (Plugin) mockPlugin.proxy();
            }
        };

        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(TEST_MODULE_COMPLETE_KEY)), moduleDescriptor);

        List<PluginResource> resources = pluginResourceLocator.getPluginResources(TEST_MODULE_COMPLETE_KEY);
        assertEquals(2, resources.size());

        BatchPluginResource ieBatch = (BatchPluginResource) resources.get(0);
        assertEquals(TEST_MODULE_COMPLETE_KEY, ieBatch.getModuleCompleteKey());
        assertEquals("css", ieBatch.getType());
        assertEquals(1, ieBatch.getParams().size());
        assertEquals("true", ieBatch.getParams().get("ieonly"));

        BatchPluginResource batch = (BatchPluginResource) resources.get(1);
        assertEquals(TEST_MODULE_COMPLETE_KEY, batch.getModuleCompleteKey());
        assertEquals("css", batch.getType());
        assertEquals(0, batch.getParams().size());
    }

    public void testGetDownloadableClasspathResource() throws Exception
    {
        String resourceName = "test.css";
        String url = "/download/resources/" + TEST_MODULE_COMPLETE_KEY + "/" + resourceName;

        Mock mockPlugin = new Mock(Plugin.class);
        Mock mockModuleDescriptor = new Mock(ModuleDescriptor.class);
        mockModuleDescriptor.expectAndReturn("getPluginKey", TEST_PLUGIN_KEY);
        mockModuleDescriptor.expectAndReturn("getResourceLocation", C.args(C.eq("download"), C.eq(resourceName)),
            new ResourceLocation("", resourceName, "download", "text/css", "", Collections.EMPTY_MAP));

        mockPluginAccessor.expectAndReturn("getPluginModule", C.args(C.eq(TEST_MODULE_COMPLETE_KEY)), mockModuleDescriptor.proxy());
        mockPluginAccessor.expectAndReturn("isPluginModuleEnabled", C.args(C.eq(TEST_MODULE_COMPLETE_KEY)), Boolean.TRUE);
        mockPluginAccessor.expectAndReturn("getPlugin", C.args(C.eq(TEST_PLUGIN_KEY)), mockPlugin.proxy());

        DownloadableResource resource = pluginResourceLocator.getDownloadableResource(url, Collections.EMPTY_MAP);

        assertTrue(resource instanceof DownloadableClasspathResource);
    }

    public void testGetDownloadableBatchResource() throws Exception
    {
        String url = "/download/batch/css/" + TEST_MODULE_COMPLETE_KEY + "/all.css";
        String ieResourceName = "master-ie.css";
        Map<String, String> params = new TreeMap<String, String>();
        params.put("ieonly", "true");

        List<ResourceDescriptor> resourceDescriptors = TestUtils.createResourceDescriptors(ieResourceName, "master.css");

        Mock mockPlugin = new Mock(Plugin.class);
        Mock mockModuleDescriptor = new Mock(ModuleDescriptor.class);
        mockModuleDescriptor.expectAndReturn("getPluginKey", TEST_PLUGIN_KEY);
        mockModuleDescriptor.expectAndReturn("getCompleteKey", TEST_MODULE_COMPLETE_KEY);
        mockModuleDescriptor.expectAndReturn("getResourceDescriptors", C.args(C.eq("download")), resourceDescriptors);
        mockModuleDescriptor.expectAndReturn("getResourceLocation", C.args(C.eq("download"), C.eq(ieResourceName)),
            new ResourceLocation("", ieResourceName, "download", "text/css", "", Collections.EMPTY_MAP));

        mockPluginAccessor.matchAndReturn("isPluginModuleEnabled", C.args(C.eq(TEST_MODULE_COMPLETE_KEY)), Boolean.TRUE);
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(TEST_MODULE_COMPLETE_KEY)), mockModuleDescriptor.proxy());
        mockPluginAccessor.matchAndReturn("getPluginModule", C.args(C.eq(TEST_MODULE_COMPLETE_KEY)), mockModuleDescriptor.proxy());
        mockPluginAccessor.matchAndReturn("getPlugin", C.args(C.eq(TEST_PLUGIN_KEY)), mockPlugin.proxy());

        DownloadableResource resource = pluginResourceLocator.getDownloadableResource(url, params);

        assertTrue(resource instanceof BatchPluginResource);
    }
}