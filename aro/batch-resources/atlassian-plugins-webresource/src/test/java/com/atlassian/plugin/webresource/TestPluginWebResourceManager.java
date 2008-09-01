package com.atlassian.plugin.webresource;

import junit.framework.TestCase;
import com.mockobjects.dynamic.Mock;
import com.mockobjects.dynamic.C;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.elements.ResourceDescriptor;

import java.util.*;
import java.io.StringWriter;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;

public class TestPluginWebResourceManager extends TestCase
{
    private static final String BASEURL = "http://www.foo.com";
    private static final String SYSTEM_COUNTER = "123";
    private static final String SYSTEM_BUILD_NUMBER = "650";

    private PluginWebResourceManager pluginWebResourceManager;
    private Mock mockWebResourceIntegration;
    Mock mockPluginAccessor;

    protected void setUp() throws Exception
    {
        super.setUp();

        mockPluginAccessor = new Mock(PluginAccessor.class);
        mockWebResourceIntegration = new Mock(WebResourceIntegration.class);
        mockWebResourceIntegration.matchAndReturn("getPluginAccessor", mockPluginAccessor.proxy());
        mockWebResourceIntegration.matchAndReturn("getBaseUrl", BASEURL);
        mockWebResourceIntegration.matchAndReturn("getSystemBuildNumber", SYSTEM_BUILD_NUMBER);
        mockWebResourceIntegration.matchAndReturn("getSystemCounter", SYSTEM_COUNTER);

        pluginWebResourceManager = new PluginWebResourceManagerImpl((WebResourceIntegration) mockWebResourceIntegration.proxy());
    }

    protected void tearDown() throws Exception
    {
        mockPluginAccessor = null;
        mockWebResourceIntegration = null;
        pluginWebResourceManager = null;

        super.tearDown();
    }

    // in single mode
    public void testWriteResource() throws Exception
    {
        final String moduleKey = "test.plugin:web-resources";

        WebResourceModuleDescriptor descriptor1 = makeDescriptor(moduleKey, 1, new String[] {"foo.css", "bar.css"});
        mockPluginAccessor.expectAndReturn("getEnabledPluginModule", C.args(C.eq(moduleKey)), descriptor1);

        StringWriter writer = new StringWriter();
        pluginWebResourceManager.writeResource(moduleKey, writer, PluginWebResourceManager.RequestMode.SINGLE);
        assertEquals(
            "<link type=\"text/css\" rel=\"stylesheet\" " +
                "href=\"http://www.foo.com/s/650/123/1/_/download/resources/test.plugin:web-resources/foo.css\" media=\"all\"/>\n" +
            "<link type=\"text/css\" rel=\"stylesheet\" " +
                "href=\"http://www.foo.com/s/650/123/1/_/download/resources/test.plugin:web-resources/bar.css\" media=\"all\"/>\n",
        writer.toString());

    }

    // in single mode
    public void testRequireResources() throws Exception
    {
        final String moduleKey = "test.plugin:web-resources";

        WebResourceModuleDescriptor descriptor1 = makeDescriptor(moduleKey, 1, new String[] {"foo.css", "bar.css"});
        mockPluginAccessor.expectAndReturn("getEnabledPluginModule", C.args(C.eq(moduleKey)), descriptor1);

        Map requiredResources = new HashMap();
        mockWebResourceIntegration.matchAndReturn("getRequestCache", requiredResources);

        pluginWebResourceManager.requireResource(moduleKey);
        assertEquals(1, ((Collection)requiredResources.get("plugin.webresource.names")).size());

        StringWriter writer = new StringWriter();
        pluginWebResourceManager.writeRequiredResources(writer, PluginWebResourceManager.RequestMode.SINGLE);
        assertEquals(
            "<link type=\"text/css\" rel=\"stylesheet\" " +
                "href=\"http://www.foo.com/s/650/123/1/_/download/resources/test.plugin:web-resources/foo.css\" media=\"all\"/>\n" +
            "<link type=\"text/css\" rel=\"stylesheet\" " +
                "href=\"http://www.foo.com/s/650/123/1/_/download/resources/test.plugin:web-resources/bar.css\" media=\"all\"/>\n",
        writer.toString());
    }

    public void testBatchRequireResources() throws Exception
    {
        final String moduleKey1 = "test.plugin:web-resources";
        final String moduleKey2 = "test.plugin2:web-resources";

        WebResourceModuleDescriptor descriptor1 = makeDescriptor(moduleKey1, 1, new String[] {"foo.css"});
        WebResourceModuleDescriptor descriptor2 = makeDescriptor(moduleKey2, 1, new String[] {"foo.js"});

        mockPluginAccessor.expectAndReturn("getEnabledPluginModule", C.args(C.eq(moduleKey1)), descriptor1);
        mockPluginAccessor.expectAndReturn("getEnabledPluginModule", C.args(C.eq(moduleKey2)), descriptor2);

        Map requiredResources = new HashMap();
        mockWebResourceIntegration.matchAndReturn("getRequestCache", requiredResources);

        pluginWebResourceManager.requireResource(moduleKey1);
        pluginWebResourceManager.requireResource(moduleKey2);

        assertEquals(2, ((Collection)requiredResources.get("plugin.webresource.names")).size());

        StringWriter writer = new StringWriter();
        pluginWebResourceManager.writeRequiredResources(writer, PluginWebResourceManager.RequestMode.BATCH);
        assertEquals(
            "<link type=\"text/css\" rel=\"stylesheet\" " +
            "href=\"http://www.foo.com/s/650/123/1/_/download/resources/css/test.plugin:web-resources\" media=\"all\"/>\n" +
            "<script type=\"text/javascript\" src=\"http://www.foo.com/s/650/123/1/_/download/resources/js/test.plugin2:web-resources\" ></script>\n",
        writer.toString());
    }

    private WebResourceModuleDescriptor makeDescriptor(final String moduleKey, int pluginVersion, String[] resources) throws Exception
    {
        final List<ResourceDescriptor> resourceDescriptors = new ArrayList<ResourceDescriptor>();
        for(String resource : resources)
        {
            Document document = DocumentHelper.parseText("<resource type=\"download\" name=\"" + resource +
                "\" location=\"/" + resource + "\" />");
            ResourceDescriptor descriptor = new ResourceDescriptor(document.getRootElement());
            resourceDescriptors.add(descriptor);
        }

        final Mock mockPlugin = new Mock(Plugin.class);
        mockPlugin.matchAndReturn("getPluginsVersion", pluginVersion);

        return new WebResourceModuleDescriptor () {
            public List getResourceDescriptors()
            {
                return resourceDescriptors;
            }

            public Plugin getPlugin()
            {
                return (Plugin) mockPlugin.proxy();
            }

            public String getCompleteKey()
            {
                return moduleKey;
            }
        };
    }
}
