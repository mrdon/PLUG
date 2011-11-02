package com.atlassian.plugin.webresource;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.elements.ResourceDescriptor;
import com.atlassian.plugin.servlet.DownloadableResource;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import junit.framework.TestCase;

public class TestSingleBatchDownloadableResourceBuilder extends TestCase
{
    public static final String MODULE_KEY = "test.plugin:webresources";
    @Mock
    private PluginAccessor mockPluginAccessor;
    @Mock
    private WebResourceUrlProvider mockWebResourceUrlProvider;
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
        builder = new SingleBatchDownloadableResourceBuilder(mockPluginAccessor, mockWebResourceUrlProvider, mockResourceFinder);

        plugin = TestUtils.createTestPlugin("test.plugin", "1.0");
    }

    @Override
    public void tearDown() throws Exception
    {
        mockPluginAccessor = null;
        mockWebResourceUrlProvider = null;
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

        addModuleDescriptor(resourceDescriptors);

        final DownloadableResource resource = builder.parse("/download/batch/en/1/" + MODULE_KEY + "/test.plugin:webresources.css",
            Collections.<String, String> emptyMap());
        final BatchPluginResource batchResource = (BatchPluginResource) resource;
        assertTrue(batchResource.isCacheSupported());

        final DownloadableResource resource2 = builder.parse("/download/batch/en/1/" + MODULE_KEY + "/test.plugin:webresources-nocache.css", queryParams);
        final BatchPluginResource batchResource2 = (BatchPluginResource) resource2;
        assertFalse(batchResource2.isCacheSupported());
    }

    public void testParse() throws Exception
    {
        final List<ResourceDescriptor> resourceDescriptors = TestUtils.createResourceDescriptors("webresources.css");
        addModuleDescriptor(resourceDescriptors);

        final DownloadableResource resource = builder.parse("/download/batch/en/1/" + MODULE_KEY + "/test.plugin:webresources.css",
            Collections.<String, String> emptyMap());

        final BatchPluginResource batchResource = (BatchPluginResource) resource;
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
        addModuleDescriptor(resourceDescriptors);

        final DownloadableResource resource = builder.parse("/download/batch/en/1/" + MODULE_KEY + "/test.plugin:webresources.css", queryParams);
        final BatchPluginResource batchResource = (BatchPluginResource) resource;
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
        addModuleDescriptor(resourceDescriptors);

        final DownloadableResource resource = builder.parse("/download/batch/en/1/" + MODULE_KEY + "/test.plugin:webresources.css", queryParams);
        final BatchPluginResource batchResource = (BatchPluginResource) resource;
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
        addModuleDescriptor(resourceDescriptors);

        final DownloadableResource resource = builder.parse("/random/stuff/download/batch/en/1/" + MODULE_KEY + "/test.plugin:webresources.css",
            queryParams);
        final BatchPluginResource batchResource = (BatchPluginResource) resource;
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

        addModuleDescriptor(resourceDescriptors);

        final String moduleKey = MODULE_KEY;
        final BatchPluginResource batchResource = new BatchPluginResource(moduleKey, "js", params, Collections.<DownloadableResource> emptyList());
        final URI uri = new URI(batchResource.getUrl());
        final BatchPluginResource parsedBatchResource = (BatchPluginResource) builder.parse(uri.getPath(), params);
        assertEquals(batchResource.getType(), parsedBatchResource.getType());
        assertEquals(batchResource.getModuleCompleteKey(), parsedBatchResource.getModuleCompleteKey());
        assertEquals(batchResource.getParams(), parsedBatchResource.getParams());
        assertEquals(moduleKey + ".js", parsedBatchResource.getResourceName());
    }

    public void testParseInvlaidUrlThrowsException()
    {
        try
        {
            builder.parse("/download/batch/en/1/blah.css", Collections.<String, String> emptyMap());
            fail("Should have thrown exception for invalid url");
        }
        catch (final UrlParseException e)
        {
            //expected
        }
    }


    private void addModuleDescriptor(List<ResourceDescriptor> resourceDescriptors)
    {
        moduleDescriptor = TestUtils.createWebResourceModuleDescriptor(MODULE_KEY, plugin, resourceDescriptors);
        when(mockPluginAccessor.getEnabledPluginModule(MODULE_KEY)).thenReturn((ModuleDescriptor) moduleDescriptor);

        for (ResourceDescriptor resourceDescriptor : resourceDescriptors)
        {
            // We don't really care what the resource is as long as the finder doesn't return null.
            DownloadableResource downloadableResourceInBatch = mock(DownloadableResource.class);
            when(mockResourceFinder.find(MODULE_KEY, resourceDescriptor.getName())).thenReturn(downloadableResourceInBatch);
        }
    }
}
