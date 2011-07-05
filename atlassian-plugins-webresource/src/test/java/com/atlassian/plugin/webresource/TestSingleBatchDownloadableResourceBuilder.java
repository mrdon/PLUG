package com.atlassian.plugin.webresource;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.elements.ResourceDescriptor;
import com.atlassian.plugin.servlet.DownloadableResource;
import junit.framework.TestCase;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.mockito.Mockito.when;

public class TestSingleBatchDownloadableResourceBuilder extends TestCase
{
    public static final String MODULE_KEY = "test.plugin:webresources";
    @Mock
    private PluginAccessor mockPluginAccessor;   
    @Mock
    private WebResourceIntegration mockWebResourceIntegration;    
    @Mock
    private DownloadableResourceFinder mockResourceFinder;

    private Plugin plugin;
    private WebResourceModuleDescriptor moduleDescriptor;
    private SingleBatchDownloadableResourceBuilder builder;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        
        MockitoAnnotations.initMocks(this);
        builder = new SingleBatchDownloadableResourceBuilder(mockPluginAccessor, mockWebResourceIntegration, mockResourceFinder);

        plugin = TestUtils.createTestPlugin("test.plugin", "1.0");
    }

    @Override
    public void tearDown() throws Exception
    {
        mockPluginAccessor = null;
        mockWebResourceIntegration = null;
        mockResourceFinder = null;
        plugin = null;
        moduleDescriptor = null;
        builder = null;
        
        super.tearDown();
    }
    
    public void testIsCacheSupported() throws Exception
    {
        final Map<String, String> queryParams = new TreeMap<String, String>();
        queryParams.put("cache", "false");
        final List<ResourceDescriptor> resourceDescriptors = TestUtils.createResourceDescriptors("webresources.css");
        resourceDescriptors.add(TestUtils.createResourceDescriptor("webresources-nocache.css", queryParams));

        moduleDescriptor = TestUtils.createWebResourceModuleDescriptor(MODULE_KEY, plugin, resourceDescriptors);
        when(mockPluginAccessor.getEnabledPluginModule(MODULE_KEY)).thenReturn((ModuleDescriptor) moduleDescriptor);

        final DownloadableResource resource = builder.parse("/download/batch/" + MODULE_KEY + "/test.plugin:webresources.css",
                Collections.<String, String>emptyMap());
        BatchPluginResource batchResource = (BatchPluginResource) resource;
        assertTrue(batchResource.isCacheSupported());

        final DownloadableResource resource2 = builder.parse("/download/batch/" + MODULE_KEY + "/test.plugin:webresources-nocache.css",
                queryParams);
        BatchPluginResource batchResource2 = (BatchPluginResource) resource2;
        assertFalse(batchResource2.isCacheSupported());
    }

    public void testParse() throws Exception
    {
        final List<ResourceDescriptor> resourceDescriptors = TestUtils.createResourceDescriptors("webresources.css");
        moduleDescriptor = TestUtils.createWebResourceModuleDescriptor(MODULE_KEY, plugin, resourceDescriptors);
        when(mockPluginAccessor.getEnabledPluginModule(MODULE_KEY)).thenReturn((ModuleDescriptor) moduleDescriptor);

        final DownloadableResource resource = builder.parse("/download/batch/" + MODULE_KEY + "/test.plugin:webresources.css",
                Collections.<String, String>emptyMap());
        
        BatchPluginResource batchResource = (BatchPluginResource) resource;
        assertEquals("css", batchResource.getType());
        assertEquals(MODULE_KEY, batchResource.getModuleCompleteKey());

        final Map<String, String> params = batchResource.getParams();

        assertEquals(0, params.size());
    }

    public void testParseWithParams() throws Exception
    {
        final Map<String, String> queryParams = new TreeMap<String, String>();
        queryParams.put("ieonly", "true");
        queryParams.put("foo", "bar");

        final List<ResourceDescriptor> resourceDescriptors = new ArrayList<ResourceDescriptor>();
        resourceDescriptors.add(TestUtils.createResourceDescriptor("webresources.css", queryParams));
        moduleDescriptor = TestUtils.createWebResourceModuleDescriptor(MODULE_KEY, plugin, resourceDescriptors);
        when(mockPluginAccessor.getEnabledPluginModule(MODULE_KEY)).thenReturn((ModuleDescriptor) moduleDescriptor);

        final DownloadableResource resource = builder.parse("/download/batch/" + MODULE_KEY + "/test.plugin:webresources.css",
                queryParams);
        BatchPluginResource batchResource = (BatchPluginResource) resource;
        assertEquals("css", batchResource.getType());
        assertEquals(MODULE_KEY, batchResource.getModuleCompleteKey());

        final Map<String, String> params = new TreeMap<String, String>();
        params.put("ieonly", "true");
        params.put("foo", "bar");

        assertEquals(params, batchResource.getParams());
    }

    public void testParseWithParams2() throws Exception
    {
        final Map<String, String> queryParams = new TreeMap<String, String>();
        queryParams.put("ieonly", "true");
        queryParams.put("foo", "bar");

        final List<ResourceDescriptor> resourceDescriptors = new ArrayList<ResourceDescriptor>();
        resourceDescriptors.add(TestUtils.createResourceDescriptor("webresources.css", queryParams));
        moduleDescriptor = TestUtils.createWebResourceModuleDescriptor(MODULE_KEY, plugin, resourceDescriptors);
        when(mockPluginAccessor.getEnabledPluginModule(MODULE_KEY)).thenReturn((ModuleDescriptor) moduleDescriptor);

        final DownloadableResource resource = builder.parse("/download/batch/" + MODULE_KEY + "/test.plugin:webresources.css?ieonly=true&foo=bar",
                queryParams);
        BatchPluginResource batchResource = (BatchPluginResource) resource;
        assertEquals("css", batchResource.getType());
        assertEquals(MODULE_KEY, batchResource.getModuleCompleteKey());

        final Map<String, String> params = new TreeMap<String, String>();
        params.put("ieonly", "true");
        params.put("foo", "bar");

        assertEquals(params, batchResource.getParams());
    }

    public void testParseWithParamsAndRandomPrefix() throws Exception
    {
        final Map<String, String> queryParams = new TreeMap<String, String>();
        queryParams.put("ieonly", "true");
        queryParams.put("foo", "bar");
        final List<ResourceDescriptor> resourceDescriptors = new ArrayList<ResourceDescriptor>();
        resourceDescriptors.add(TestUtils.createResourceDescriptor("webresources.css", queryParams));
        moduleDescriptor = TestUtils.createWebResourceModuleDescriptor(MODULE_KEY, plugin, resourceDescriptors);
        when(mockPluginAccessor.getEnabledPluginModule(MODULE_KEY)).thenReturn((ModuleDescriptor) moduleDescriptor);

        final DownloadableResource resource = builder.parse("/random/stuff/download/batch/" + MODULE_KEY + "/test.plugin:webresources.css?ieonly=true&foo=bar",
                queryParams);
        BatchPluginResource batchResource = (BatchPluginResource) resource;
        assertEquals("css", batchResource.getType());
        assertEquals(MODULE_KEY, batchResource.getModuleCompleteKey());

        final Map<String, String> params = new TreeMap<String, String>();
        params.put("ieonly", "true");
        params.put("foo", "bar");

        assertEquals(params, batchResource.getParams());
    }

    public void testRoundTrip() throws Exception
    {
        final Map<String, String> params = new TreeMap<String, String>();
        params.put("media", "print");

        final List<ResourceDescriptor> resourceDescriptors = new ArrayList<ResourceDescriptor>();
        resourceDescriptors.add(TestUtils.createResourceDescriptor("webresources.js", params));
        moduleDescriptor = TestUtils.createWebResourceModuleDescriptor(MODULE_KEY, plugin, resourceDescriptors);
        when(mockPluginAccessor.getEnabledPluginModule(MODULE_KEY)).thenReturn((ModuleDescriptor) moduleDescriptor);

        final String moduleKey = MODULE_KEY;
        final BatchPluginResource batchResource = new BatchPluginResource(moduleKey, "js", params);
        final String url = batchResource.getUrl();
        final DownloadableResource parsedResource = builder.parse(url, params);

        BatchPluginResource parsedBatchResource = (BatchPluginResource) parsedResource;
        assertEquals(batchResource.getType(), parsedBatchResource.getType());
        assertEquals(batchResource.getModuleCompleteKey(), parsedBatchResource.getModuleCompleteKey());
        assertEquals(batchResource.getParams(), parsedBatchResource.getParams());
        assertEquals(moduleKey + ".js", parsedBatchResource.getResourceName());
    }

    public void testParseInvlaidUrlThrowsException()
    {
        try
        {
            builder.parse("/download/batch/blah.css", Collections.<String, String>emptyMap());
            fail("Should have thrown exception for invalid url");
        }
        catch (UrlParseException e)
        {
            //expected
        }
    }    
}