package com.atlassian.plugins.resourcedownload;

import junit.framework.TestCase;
import com.atlassian.plugin.resourcedownload.PluginResourceLocatorImpl;
import com.atlassian.plugin.resourcedownload.WebResourceIntegration;
import com.atlassian.plugin.resourcedownload.ContentTypeResolver;
import com.atlassian.plugin.resourcedownload.BatchResource;
import com.atlassian.plugin.resourcedownload.Resource;
import com.atlassian.plugin.resourcedownload.WebResourceModuleDescriptor;
import com.atlassian.plugin.resourcedownload.servlet.ServletContextFactory;
import com.atlassian.plugin.resourcedownload.servlet.DownloadableResource;
import com.atlassian.plugin.resourcedownload.servlet.ClasspathResource;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.elements.ResourceLocation;
import com.atlassian.plugin.elements.ResourceDescriptor;
import com.mockobjects.dynamic.Mock;
import com.mockobjects.dynamic.C;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;

public class TestPluginResourceLocatorImpl extends TestCase
{
    private static final String BASEURL = "http://www.foo.com";
    private static final String SYSTEM_COUNTER = "123";
    private static final String SYSTEM_BUILD_NUMBER = "650";

    private PluginResourceLocatorImpl pluginResourceLocator;
    private Mock mockPluginAccessor;
    private Mock mockWebResourceIntegration;
    private Mock mockContentTypeResolver;
    private Mock mockServletContextFactory;

    protected void setUp() throws Exception
    {
        super.setUp();

        mockPluginAccessor = new Mock(PluginAccessor.class);
        mockWebResourceIntegration = new Mock(WebResourceIntegration.class);
        mockContentTypeResolver = new Mock(ContentTypeResolver.class);
        mockServletContextFactory = new Mock(ServletContextFactory.class);

        mockWebResourceIntegration.matchAndReturn("getBaseUrl", BASEURL);
        mockWebResourceIntegration.matchAndReturn("getSystemBuildNumber", SYSTEM_BUILD_NUMBER);
        mockWebResourceIntegration.matchAndReturn("getSystemCounter", SYSTEM_COUNTER);

        pluginResourceLocator = new PluginResourceLocatorImpl(
            (PluginAccessor) mockPluginAccessor.proxy(),
            (WebResourceIntegration) mockWebResourceIntegration.proxy(),
            (ContentTypeResolver) mockContentTypeResolver.proxy(),
            (ServletContextFactory) mockServletContextFactory.proxy()
        );
    }

    protected void tearDown() throws Exception
    {
        pluginResourceLocator = null;
        mockPluginAccessor = null;
        mockWebResourceIntegration = null;
        mockContentTypeResolver = null;
        mockServletContextFactory = null;

        super.tearDown();
    }

    public void testLocateByCompleteKeyWithBatching() throws Exception
    {
        final String pluginKey = "test.plugin";
        final String webResourceModule = "web-resources";
        final String completeKey = pluginKey + ":" + webResourceModule;

        final Mock mockPlugin = new Mock(Plugin.class);
        mockPlugin.matchAndReturn("getPluginsVersion", 1);

        final List<ResourceDescriptor> resourceDescriptors = new ArrayList<ResourceDescriptor>();
        resourceDescriptors.add(createResourceDescriptor("master-ie.css"));
        resourceDescriptors.add(createResourceDescriptor("master.css"));
        resourceDescriptors.add(createResourceDescriptor("comments.css"));

        WebResourceModuleDescriptor moduleDescriptor = new WebResourceModuleDescriptor() {
            public String getCompleteKey()
            {
                return completeKey;
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

        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(completeKey)), moduleDescriptor);

        List<Resource> resources = pluginResourceLocator.locateByCompleteKey(completeKey);
        assertEquals(2, resources.size());

        BatchResource ieBatch = (BatchResource) resources.get(0);
        assertEquals(completeKey, ieBatch.getModuleCompleteKey());
        assertEquals("css", ieBatch.getType());
        assertEquals(1, ieBatch.getParams().size());
        assertEquals("true", ieBatch.getParams().get("ieonly"));

        BatchResource batch = (BatchResource) resources.get(1);
        assertEquals(completeKey, batch.getModuleCompleteKey());
        assertEquals("css", batch.getType());
        assertEquals(0, batch.getParams().size());
    }

    public void testLocateByUrlForClasspathResource() throws Exception
    {
        String pluginKey = "test.plugin";
        String webResourceModule = "web-resources";
        String resourceName = "test.css";
        String completeKey = pluginKey + ":" + webResourceModule;
        String url = "/download/resources/" + completeKey + "/" + resourceName;

        Mock mockPlugin = new Mock(Plugin.class);
        Mock mockModuleDescriptor = new Mock(ModuleDescriptor.class);
        mockModuleDescriptor.expectAndReturn("getPluginKey", pluginKey);
        mockModuleDescriptor.expectAndReturn("getResourceLocation", C.args(C.eq("download"), C.eq(resourceName)),
            new ResourceLocation("", resourceName, "download", "text/css", "", Collections.EMPTY_MAP));

        mockPluginAccessor.expectAndReturn("getPluginModule", C.args(C.eq(completeKey)), mockModuleDescriptor.proxy());
        mockPluginAccessor.expectAndReturn("isPluginModuleEnabled", C.args(C.eq(completeKey)), Boolean.TRUE);
        mockPluginAccessor.expectAndReturn("getPlugin", C.args(C.eq(pluginKey)), mockPlugin.proxy());

        DownloadableResource resource = pluginResourceLocator.locateByUrl(url);

        assertTrue(resource instanceof ClasspathResource);
    }

    public void testLocateByUrlForBatchResource() throws Exception
    {
        String pluginKey = "test.plugin";
        String webResourceModule = "web-resources";
        String completeKey = pluginKey + ":" + webResourceModule;
        String url = "/download/batch/css/" + completeKey + "/all.css?ieonly=true";
        String ieResourceName = "master-ie.css";

        List<ResourceDescriptor> resourceDescriptors = new ArrayList<ResourceDescriptor>();
        resourceDescriptors.add(createResourceDescriptor(ieResourceName));
        resourceDescriptors.add(createResourceDescriptor("master.css"));

        Mock mockPlugin = new Mock(Plugin.class);
        Mock mockModuleDescriptor = new Mock(ModuleDescriptor.class);
        mockModuleDescriptor.expectAndReturn("getPluginKey", pluginKey);
        mockModuleDescriptor.expectAndReturn("getCompleteKey", completeKey);
        mockModuleDescriptor.expectAndReturn("getResourceDescriptors", C.args(C.eq("download")), resourceDescriptors);
        mockModuleDescriptor.expectAndReturn("getResourceLocation", C.args(C.eq("download"), C.eq(ieResourceName)),
            new ResourceLocation("", ieResourceName, "download", "text/css", "", Collections.EMPTY_MAP));

        mockPluginAccessor.matchAndReturn("isPluginModuleEnabled", C.args(C.eq(completeKey)), Boolean.TRUE);
        mockPluginAccessor.matchAndReturn("getEnabledPluginModule", C.args(C.eq(completeKey)), mockModuleDescriptor.proxy());
        mockPluginAccessor.matchAndReturn("getPluginModule", C.args(C.eq(completeKey)), mockModuleDescriptor.proxy());
        mockPluginAccessor.matchAndReturn("getPlugin", C.args(C.eq(pluginKey)), mockPlugin.proxy());

        DownloadableResource resource = pluginResourceLocator.locateByUrl(url);

        assertTrue(resource instanceof BatchResource);
    }

    private ResourceDescriptor createResourceDescriptor(String resourceName) throws DocumentException
    {
        String xml = "<resource type=\"download\" name=\"" + resourceName + "\" location=\"/includes/css/" + resourceName + "\">\n" +
                            "<param name=\"source\" value=\"webContext\"/>\n";

        if(resourceName.indexOf("ie") != -1)
           xml += "<param name=\"ieonly\" value=\"true\"/>\n";

        xml += "</resource>";
        return new ResourceDescriptor(DocumentHelper.parseText(xml).getRootElement());
    }
}
