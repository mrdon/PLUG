package com.atlassian.plugin.webresource;

import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.servlet.DownloadableResource;
import junit.framework.TestCase;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import static org.mockito.Mockito.when;

public class TestSuperBatchDownloadableResourceBuilder extends TestCase
{
    @Mock
    private DefaultResourceDependencyResolver mockDependencyResolver;
    @Mock
    private PluginAccessor mockPluginAccessor;
    @Mock
    private WebResourceUrlProvider mockWebResourceUrlProvider;
    @Mock
    private DownloadableResourceFinder mockResourceFinder;

    private SuperBatchDownloadableResourceBuilder builder;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        MockitoAnnotations.initMocks(this);
        builder = new SuperBatchDownloadableResourceBuilder(mockDependencyResolver, mockPluginAccessor, mockWebResourceUrlProvider, mockResourceFinder, null);

        when(mockDependencyResolver.getSuperBatchDependencies()).thenReturn(Collections.<WebResourceModuleDescriptor>emptyList());
    }

    @Override
    public void tearDown() throws Exception
    {
        mockDependencyResolver = null;
        mockPluginAccessor = null;
        mockWebResourceUrlProvider = null;
        mockResourceFinder = null;

        super.tearDown();
    }

    public void testParseCss() throws UrlParseException
    {
        String path = "/superbatch/css/batch.css";
        assertTrue(builder.matches("/download" +path));
        DownloadableResource resource = builder.parse(path, Collections.<String, String>emptyMap());
        SuperBatchPluginResource batchResource = (SuperBatchPluginResource) resource;
        assertEquals("css", batchResource.getType());
        assertEquals(path, batchResource.getUrl());
        assertEquals("batch.css", batchResource.getResourceName());
    }

    // For some reason the download manager doesn't strip context paths before sending it in to be matched.
    public void testParseWithContextPath()
    {
        assertTrue(builder.matches("/confluence/download/superbatch/css/batch.css"));
    }

    public void testParseJavascript() throws UrlParseException
    {
        String path = "/superbatch/js/batch.js";
        assertTrue(builder.matches("/download" + path));
        DownloadableResource resource = builder.parse(path, Collections.<String, String>emptyMap());
        SuperBatchPluginResource batchResource = (SuperBatchPluginResource) resource;
        assertEquals("js", batchResource.getType());
        assertEquals(path, batchResource.getUrl());
        assertEquals("batch.js", batchResource.getResourceName());
    }

    public void testParseWithParam() throws UrlParseException
    {
        String path = "/superbatch/js/batch.js";
        Map<String, String> params = Collections.singletonMap("ieOnly", "true");
        DownloadableResource resource = builder.parse(path, params);
        SuperBatchPluginResource batchResource = (SuperBatchPluginResource) resource;
        assertEquals(params, batchResource.getParams());
        assertEquals(path + "?ieOnly=true", batchResource.getUrl());
        assertEquals("batch.js", batchResource.getResourceName());
    }

    public void testParseWithParams() throws UrlParseException
    {
        String path = "/superbatch/js/batch.js";
        Map<String, String> params = new TreeMap<String, String>();
        params.put("ieOnly", "true");
        params.put("zomg", "false");
        DownloadableResource resource = builder.parse(path, params);
        SuperBatchPluginResource batchResource = (SuperBatchPluginResource) resource;
        assertEquals(params, batchResource.getParams());
        assertEquals(path + "?ieOnly=true&zomg=false", batchResource.getUrl());
        assertEquals("batch.js", batchResource.getResourceName());
    }

    public void testNotSuperbatches()
    {
        assertFalse("wrong path", builder.matches("/download/superbitch/css/batch.css"));
        assertFalse("wrong path", builder.matches("/download/superbatch/css/images/foo.png"));
    }

}
