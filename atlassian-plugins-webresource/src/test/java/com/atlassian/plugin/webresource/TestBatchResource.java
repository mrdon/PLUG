package com.atlassian.plugin.webresource;

import junit.framework.TestCase;

import java.util.Map;
import java.util.TreeMap;
import java.util.Collections;

public class TestBatchResource extends TestCase
{
    public void testIsCacheSupported()
    {
        BatchPluginResource resource = BatchPluginResource.parse("/download/batch/css/test.plugin:webresources.css", Collections.EMPTY_MAP);
        assertTrue(resource.isCacheSupported());

        Map queryParams = new TreeMap<String, String>();
        queryParams.put("cache", "false");
        BatchPluginResource resource2 = BatchPluginResource.parse("/download/batch/css/test.plugin:webresources.css", queryParams);
        assertFalse(resource2.isCacheSupported());
    }

    public void testParse()
    {
        BatchPluginResource resource = BatchPluginResource.parse("/download/batch/css/test.plugin:webresources.css", Collections.EMPTY_MAP);
        assertEquals("css", resource.getType());
        assertEquals("test.plugin:webresources", resource.getModuleCompleteKey());

        Map<String, String> params = resource.getParams();

        assertEquals(0, params.size());
    }

    public void testParseWithParams()
    {
        Map queryParams = new TreeMap<String, String>();
        queryParams.put("ieonly", "true");
        queryParams.put("foo", "bar");

        BatchPluginResource resource = BatchPluginResource.parse("/download/batch/css/test.plugin:webresources.css", queryParams);
        assertEquals("css", resource.getType());
        assertEquals("test.plugin:webresources", resource.getModuleCompleteKey());

        Map<String, String> params = new TreeMap<String, String>();
        params.put("ieonly", "true");
        params.put("foo", "bar");

        assertEquals(params, resource.getParams());
    }

    public void testGetUrl()
    {
        BatchPluginResource resource = new BatchPluginResource("test.plugin:webresources", "js", Collections.EMPTY_MAP);
        assertEquals("/download/batch/js/test.plugin:webresources.js", resource.getUrl());
    }

    public void testGetUrlWithParams()
    {
        Map<String, String> params = new TreeMap<String, String>();
        params.put("foo", "bar");
        params.put("moo", "cow");

        BatchPluginResource resource = new BatchPluginResource("test.plugin:webresources", "js", params);
        assertEquals("/download/batch/js/test.plugin:webresources.js?foo=bar&moo=cow", resource.getUrl());
    }

    public void testRoundTrip()
    {
        Map<String, String> params = new TreeMap<String, String>();
        params.put("foo", "bar");

        String moduleKey = "test.plugin:webresources";
        BatchPluginResource resource = new BatchPluginResource(moduleKey, "js", params);
        String url = resource.getUrl();
        BatchPluginResource parsedResource = BatchPluginResource.parse(url, params);

        assertEquals(resource.getType(), parsedResource.getType());
        assertEquals(resource.getModuleCompleteKey(), parsedResource.getModuleCompleteKey());
        assertEquals(resource.getParams(), parsedResource.getParams());
        assertEquals(moduleKey + ".js", resource.getResourceName());
    }

    public void testEquals()
    {
        String moduleKey = "test.plugin:webresources";
        String type = "js";

        Map<String, String> params1 = new TreeMap<String, String>();
        params1.put("key", "value");
        params1.put("foo", "bar");
        BatchPluginResource batch1 = new BatchPluginResource(moduleKey, type, params1);

        Map<String, String> params2 = new TreeMap<String, String>();
        params2.put("key", "value");
        params2.put("foo", "bar");
        BatchPluginResource batch2 = new BatchPluginResource(moduleKey, type, params2);

        Map<String, String> params3 = new TreeMap<String, String>();
        params3.put("key", "value");
        params3.put("foo", "bart");
        BatchPluginResource batch3 = new BatchPluginResource(moduleKey, type, params3);

        assertEquals(batch1, batch2);
        assertNotSame(batch1, batch3);
    }
}
