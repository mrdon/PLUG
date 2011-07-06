package com.atlassian.plugin.webresource;

import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.servlet.DownloadableResource;
import junit.framework.TestCase;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public class TestContextBatchDownloadableResourceBuilder extends TestCase
{
    @Mock
    private DefaultResourceDependencyResolver mockDependencyResolver;
    @Mock
    private PluginAccessor mockPluginAccessor;   
    @Mock
    private WebResourceUrlProvider mockWebResourceUrlProvider;
    @Mock
    private DownloadableResourceFinder mockResourceFinder;
    
    private ContextBatchDownloadableResourceBuilder builder;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        
        MockitoAnnotations.initMocks(this);
        builder = new ContextBatchDownloadableResourceBuilder(mockDependencyResolver, mockPluginAccessor, mockWebResourceUrlProvider, mockResourceFinder);
    }

    @Override
    public void tearDown() throws Exception
    {
        mockDependencyResolver = null;
        mockPluginAccessor = null;
        mockWebResourceUrlProvider = null;
        mockResourceFinder = null;
        builder = null;
        
        super.tearDown();
    }
    
    public void testParseCss() throws UrlParseException
    {
        String path = "/download/contextbatch/css/contexta/batch.css";
        assertTrue(builder.matches(path));
        DownloadableResource resource = builder.parse(path, Collections.<String, String>emptyMap());
        ContextBatchPluginResource batchResource = (ContextBatchPluginResource) resource;
        assertEquals("css", batchResource.getType());
        assertEquals(batchResource.getUrl(), path);
        assertEquals("batch.css", batchResource.getResourceName());
        assertEquals(1, batchResource.getContexts().size());
        assertEquals("contexta", batchResource.getContexts().get(0));
    }

    public void testParseCssWithMultipleContexts() throws UrlParseException
    {
        String path = "/download/contextbatch/css/contexta,contextb/batch.css";
        assertTrue(builder.matches(path));
        DownloadableResource resource = builder.parse(path, Collections.<String, String>emptyMap());
        ContextBatchPluginResource batchResource = (ContextBatchPluginResource) resource;
        assertEquals("css", batchResource.getType());
        assertEquals(batchResource.getUrl(), path);
        assertEquals("batch.css", batchResource.getResourceName());
        assertEquals(2, batchResource.getContexts().size());
        assertEquals("contexta", batchResource.getContexts().get(0));
        assertEquals("contextb", batchResource.getContexts().get(1));
    }

    public void testParseJavascript() throws UrlParseException
    {
        String path = "/download/contextbatch/js/contexta/batch.js";
        assertTrue(builder.matches(path));
        DownloadableResource resource = builder.parse(path, Collections.<String, String>emptyMap());
        ContextBatchPluginResource batchResource = (ContextBatchPluginResource) resource;
        assertEquals("js", batchResource.getType());
        assertEquals(batchResource.getUrl(), path);
        assertEquals("batch.js", batchResource.getResourceName());
        assertEquals(1, batchResource.getContexts().size());
        assertEquals("contexta", batchResource.getContexts().get(0));
    }

    public void testParseWithParam() throws UrlParseException
    {
        String path="/download/contextbatch/js/contexta/batch.js";
        Map<String, String> params = Collections.singletonMap("ieOnly", "true");
        DownloadableResource resource = builder.parse(path, params);
        ContextBatchPluginResource batchResource = (ContextBatchPluginResource) resource;
        assertEquals(params, batchResource.getParams());
        assertEquals(path + "?ieOnly=true", batchResource.getUrl());
        assertEquals("batch.js", batchResource.getResourceName());
    }

    public void testParseWithParams() throws UrlParseException
    {
        String path="/download/contextbatch/js/contexta/batch.js";
        Map<String, String> params = new TreeMap<String, String>();
        params.put("ieOnly", "true");
        params.put("zomg", "false");
        DownloadableResource resource = builder.parse(path, params);
        ContextBatchPluginResource batchResource = (ContextBatchPluginResource) resource;
        assertEquals(params, batchResource.getParams());
        assertEquals(path + "?ieOnly=true&zomg=false", batchResource.getUrl());
        assertEquals("batch.js", batchResource.getResourceName());
    }

    public void testMatch()
    {
        assertTrue("correct path", builder.matches("/download/contextbatch/css/contexta/batch.css"));
        assertFalse("wrong path", builder.matches("/download/contextbatch/js/contexta/batch.css"));
        assertFalse("wrong path", builder.matches("/download/contextbatch/css/contexta/image.png"));
    }    
}
