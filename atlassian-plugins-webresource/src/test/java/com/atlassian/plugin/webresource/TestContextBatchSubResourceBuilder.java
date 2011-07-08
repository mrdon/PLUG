package com.atlassian.plugin.webresource;

import com.atlassian.plugin.servlet.DownloadableResource;
import junit.framework.TestCase;
import org.dom4j.DocumentException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestContextBatchSubResourceBuilder extends TestCase
{
    @Mock
    private DefaultResourceDependencyResolver mockDependencyResolver;
    @Mock
    private DownloadableResourceFinder mockResourceFinder;

    private ContextBatchSubResourceBuilder builder;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        MockitoAnnotations.initMocks(this);
        builder = new ContextBatchSubResourceBuilder(mockDependencyResolver, mockResourceFinder);
    }

    @Override
    public void tearDown() throws Exception
    {
        mockDependencyResolver = null;
        mockResourceFinder = null;

        builder = null;

        super.tearDown();
    }

    public void testMatches()
    {
        assertTrue(builder.matches("/download/contextbatch/css/contexta/images/foo.png"));
        assertFalse(builder.matches("/download/contextbatch/css/contexta/batch.js"));
        assertFalse(builder.matches("/download/superbatch/css/batch.js"));
    }

    public void testParsePluginResource() throws UrlParseException
    {
        String path = "/download/contextbatch/css/contexta/images/foo.png";

        String moduleKey = "contextb-resources";
        when(mockDependencyResolver.getDependenciesInContext("contexta")).thenReturn(Arrays.asList(moduleKey));

        assertTrue(builder.matches(path));
        DownloadableResource resource = builder.parse(path, Collections.<String, String>emptyMap());
        BatchSubResource batchResource = (BatchSubResource) resource;
        assertEquals("images/foo.png", batchResource.getResourceName());
        assertTrue(batchResource.isEmpty());

        verify(mockResourceFinder).find(moduleKey, "images/foo.png");
    }

    public void testParsePluginResourceAndResolve() throws UrlParseException, DocumentException
    {
        String path = "/download/contextbatch/css/contextb/images/foo.png";

        String moduleKey = "contextb-resources";
        DownloadableResource imageResource = mock(DownloadableResource.class);
        when(mockResourceFinder.find(moduleKey, "images/foo.png")).thenReturn(imageResource);

        List<String> contextDependencies = new ArrayList<String>();
        contextDependencies.add(moduleKey);
        when(mockDependencyResolver.getDependenciesInContext("contextb")).thenReturn(contextDependencies);

        DownloadableResource resource = builder.parse(path, Collections.<String, String>emptyMap());
        BatchSubResource batchResource = (BatchSubResource) resource;
        assertEquals("images/foo.png", batchResource.getResourceName());
        assertFalse(batchResource.isEmpty());

        verify(mockResourceFinder).find(moduleKey, "images/foo.png");
    }

    public void testParsePluginResourceAndResolveMultipleContexts() throws UrlParseException, DocumentException
    {
        String path = "/download/contextbatch/css/contexta,contextb/images/foo.png";

        String moduleKey = "contextb-resources";
        DownloadableResource imageResource = mock(DownloadableResource.class);
        when(mockResourceFinder.find(moduleKey, "images/foo.png")).thenReturn(imageResource);

        List<String> contextDependencies = new ArrayList<String>();
        contextDependencies.add(moduleKey);
        when(mockDependencyResolver.getDependenciesInContext("contextb")).thenReturn(contextDependencies);
        when(mockDependencyResolver.getDependenciesInContext("contexta")).thenReturn(Collections.<String>emptyList());

        DownloadableResource resource = builder.parse(path, Collections.<String, String>emptyMap());
        BatchSubResource batchResource = (BatchSubResource) resource;
        assertEquals("images/foo.png", batchResource.getResourceName());
        assertFalse(batchResource.isEmpty());

        verify(mockResourceFinder).find(moduleKey, "images/foo.png");
    }
}
