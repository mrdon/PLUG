package com.atlassian.plugin.webresource;

import com.atlassian.plugin.servlet.DownloadableResource;
import junit.framework.TestCase;
import org.dom4j.DocumentException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.LinkedHashSet;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestSuperBatchSubResourceBuilder extends TestCase
{
    @Mock
    private DefaultResourceDependencyResolver mockDependencyResolver;
    @Mock
    private DownloadableResourceFinder mockResourceFinder;

    private SuperBatchSubResourceBuilder builder;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        MockitoAnnotations.initMocks(this);
        builder = new SuperBatchSubResourceBuilder(mockDependencyResolver, mockResourceFinder);
    }

    @Override
    public void tearDown() throws Exception
    {
        mockDependencyResolver = null;
        mockResourceFinder = null;

        builder = null;

        super.tearDown();
    }

    public void testParsePluginResource() throws UrlParseException
    {
        String path = "/download/superbatch/css/images/foo.png";
        assertTrue(builder.matches(path));
        DownloadableResource resource = builder.parse(path, Collections.<String, String>emptyMap());
        BatchSubResource batchResource = (BatchSubResource) resource;
        assertEquals("css/images/foo.png", batchResource.getResourceName());
        assertTrue(batchResource.isEmpty());
    }

    public void testParsePluginResourceAndResolve() throws UrlParseException, DocumentException
    {
        String path = "/download/superbatch/css/images/foo.png";

        String moduleKey = "super-resources";
        DownloadableResource imageResource = mock(DownloadableResource.class);
        when(mockResourceFinder.find(moduleKey, "css/images/foo.png")).thenReturn(imageResource);

        LinkedHashSet<String> superBatchDependencies = new LinkedHashSet<String>();
        superBatchDependencies.add(moduleKey);
        when(mockDependencyResolver.getSuperBatchDependencies()).thenReturn(superBatchDependencies);

        DownloadableResource resource = builder.parse(path, Collections.<String, String>emptyMap());
        BatchSubResource batchResource = (BatchSubResource) resource;
        assertEquals("css/images/foo.png", batchResource.getResourceName());
        assertFalse(batchResource.isEmpty());

        verify(mockResourceFinder).find(moduleKey, "css/images/foo.png");
    }
}
