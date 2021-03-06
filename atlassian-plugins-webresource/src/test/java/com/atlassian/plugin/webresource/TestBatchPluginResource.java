package com.atlassian.plugin.webresource;

import static java.util.Collections.emptyMap;

import com.atlassian.plugin.servlet.DownloadException;
import com.atlassian.plugin.servlet.DownloadableResource;
import com.atlassian.plugin.servlet.util.CapturingHttpServletResponse;
import com.atlassian.plugin.webresource.util.DownloadableResourceTestImpl;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import junit.framework.TestCase;

public class TestBatchPluginResource extends TestCase
{
    public void testGetUrl()
    {
        final BatchPluginResource resource = new BatchPluginResource("test.plugin:webresources", "js", Collections.<String, String> emptyMap(),
            Collections.<DownloadableResource> emptyList());
        assertEquals("/download/batch/locale/0.0/test.plugin:webresources/test.plugin:webresources.js", resource.getUrl());
    }

    public void testGetUrlWithParams()
    {
        final Map<String, String> params = new TreeMap<String, String>();
        params.put("conditionalComment", "lt IE 9");
        params.put("foo", "bar");

        final BatchPluginResource resource = new BatchPluginResource("test.plugin:webresources", "js", params,
            Collections.<DownloadableResource> emptyList());
        assertEquals("/download/batch/locale/0.0/test.plugin:webresources/test.plugin:webresources.js?conditionalComment=lt+IE+9&foo=bar", resource.getUrl());
    }

    public void testEquals()
    {
        final String moduleKey = "test.plugin:webresources";
        final String type = "js";

        final Map<String, String> params1 = new TreeMap<String, String>();
        params1.put("key", "value");
        params1.put("foo", "bar");
        final BatchPluginResource batch1 = new BatchPluginResource(moduleKey, type, params1, Collections.<DownloadableResource> emptyList());

        final Map<String, String> params2 = new TreeMap<String, String>();
        params2.put("key", "value");
        params2.put("foo", "bar");
        final BatchPluginResource batch2 = new BatchPluginResource(moduleKey, type, params2, Collections.<DownloadableResource> emptyList());

        final Map<String, String> params3 = new TreeMap<String, String>();
        params3.put("key", "value");
        params3.put("foo", "bart");
        final BatchPluginResource batch3 = new BatchPluginResource(moduleKey, type, params3, Collections.<DownloadableResource> emptyList());

        assertEquals(batch1, batch2);
        assertNotSame(batch1, batch3);
    }

    public void testNewLineStreamingHttpResponse() throws DownloadException
    {

        final DownloadableResource testResource1 = new DownloadableResourceTestImpl("text/js", "Test1");
        final DownloadableResource testResource2 = new DownloadableResourceTestImpl("text/js", "Test2");
        final List<DownloadableResource> resources = Arrays.asList(testResource1, testResource2);

        final Map<String, String> empty = emptyMap();
        final BatchPluginResource batchResource = new BatchPluginResource("test.plugin:webresources", "js", empty, resources);

        final CapturingHttpServletResponse response = new CapturingHttpServletResponse();
        batchResource.serveResource(null, response);

        final String actualResponse = response.toString();
        assertEquals("Test1\nTest2\n", actualResponse);
    }

    public void testNewLineStreamingOutputStream() throws DownloadException
    {
        final DownloadableResource testResource1 = new DownloadableResourceTestImpl("text/js", "Test1");
        final DownloadableResource testResource2 = new DownloadableResourceTestImpl("text/js", "Test2");
        final List<DownloadableResource> resources = Arrays.asList(testResource1, testResource2);

        final Map<String, String> empty = emptyMap();
        final BatchPluginResource batchResource = new BatchPluginResource("test.plugin:webresources", "js", empty, resources);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        batchResource.streamResource(baos);

        final String actualResponse = baos.toString();
        assertEquals("Test1\nTest2\n", actualResponse);
    }
}
