package com.atlassian.plugin.webresource;

import static com.google.common.collect.Iterables.get;
import static com.google.common.collect.Iterables.size;

import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.servlet.DownloadableResource;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import junit.framework.TestCase;

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
        builder = new ContextBatchDownloadableResourceBuilder(mockDependencyResolver, mockPluginAccessor, mockWebResourceUrlProvider,
            mockResourceFinder);
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
        final String path = "/download/contextbatch/css/contexta/batch.css";
        assertTrue(builder.matches(path));
        final DownloadableResource resource = builder.parse(path, Collections.<String, String> emptyMap());
        final ContextBatchPluginResource batchResource = (ContextBatchPluginResource) resource;
        assertEquals("css", batchResource.getType());
        assertEquals(batchResource.getUrl(), path);
        assertEquals("batch.css", batchResource.getResourceName());
        assertEquals(1, size(batchResource.getContexts()));
        assertEquals("contexta", get(batchResource.getContexts(), 0));
    }

    public void testParseCssWithMultipleContexts() throws UrlParseException
    {
        final String path = "/download/contextbatch/css/contexta,contextb/batch.css";
        assertTrue(builder.matches(path));
        final DownloadableResource resource = builder.parse(path, Collections.<String, String> emptyMap());
        final ContextBatchPluginResource batchResource = (ContextBatchPluginResource) resource;
        assertEquals("css", batchResource.getType());
        assertEquals(batchResource.getUrl(), path);
        assertEquals("batch.css", batchResource.getResourceName());
        assertEquals(2, size(batchResource.getContexts()));
        assertEquals("contexta", get(batchResource.getContexts(), 0));
        assertEquals("contextb", get(batchResource.getContexts(), 1));
    }

    public void testParseJavascript() throws UrlParseException
    {
        final String path = "/download/contextbatch/js/contexta/batch.js";
        assertTrue(builder.matches(path));
        final DownloadableResource resource = builder.parse(path, Collections.<String, String> emptyMap());
        final ContextBatchPluginResource batchResource = (ContextBatchPluginResource) resource;
        assertEquals("js", batchResource.getType());
        assertEquals(batchResource.getUrl(), path);
        assertEquals("batch.js", batchResource.getResourceName());
        assertEquals(1, size(batchResource.getContexts()));
        assertEquals("contexta", get(batchResource.getContexts(), 0));
    }

    public void testParseWithParam() throws UrlParseException
    {
        final String path = "/download/contextbatch/js/contexta/batch.js";
        final Map<String, String> params = Collections.singletonMap("ieOnly", "true");
        final DownloadableResource resource = builder.parse(path, params);
        final ContextBatchPluginResource batchResource = (ContextBatchPluginResource) resource;
        assertEquals(params, batchResource.getParams());
        assertEquals(path + "?ieOnly=true", batchResource.getUrl());
        assertEquals("batch.js", batchResource.getResourceName());
    }

    public void testParseWithParams() throws UrlParseException
    {
        final String path = "/download/contextbatch/js/contexta/batch.js";
        final Map<String, String> params = new TreeMap<String, String>();
        params.put("ieOnly", "true");
        params.put("zomg", "false");
        final DownloadableResource resource = builder.parse(path, params);
        final ContextBatchPluginResource batchResource = (ContextBatchPluginResource) resource;
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
