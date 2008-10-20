package com.atlassian.plugins.resourcedownload;

import junit.framework.TestCase;

import java.util.Map;
import java.util.TreeMap;
import java.util.Collections;

import com.atlassian.plugin.resourcedownload.BatchResource;

public class TestBatchResource extends TestCase
{
    public void testParse()
    {
        BatchResource resource = BatchResource.parse("/download/batch/css/test.plugin:webresources/all.css");
        assertEquals("css", resource.getType());
        assertEquals("test.plugin:webresources", resource.getModuleCompleteKey());

        Map<String, String> params = resource.getParams();

        assertEquals(0, params.size());
    }

    public void testParseWithParams()
    {
        BatchResource resource = BatchResource.parse("/download/batch/css/test.plugin:webresources/all.css?ieonly=true&foo=bar");
        assertEquals("css", resource.getType());
        assertEquals("test.plugin:webresources", resource.getModuleCompleteKey());

        Map<String, String> params = new TreeMap<String, String>();
        params.put("ieonly", "true");
        params.put("foo", "bar");

        assertEquals(params, resource.getParams());
    }

    public void testGetUrl()
    {
        BatchResource resource = new BatchResource("test.plugin:webresources", "js", Collections.EMPTY_MAP, "");
        assertEquals("/download/batch/js/test.plugin:webresources/all.js", resource.getUrl());
    }

    public void testGetUrlWithParams()
    {
        Map<String, String> params = new TreeMap<String, String>();
        params.put("foo", "bar");
        params.put("moo", "cow");

        BatchResource resource = new BatchResource("test.plugin:webresources", "js", params, "");
        assertEquals("/download/batch/js/test.plugin:webresources/all.js?foo=bar&moo=cow", resource.getUrl());
    }

    public void testRoundTrip()
    {
        Map<String, String> params = new TreeMap<String, String>();
        params.put("foo", "bar");

        BatchResource resource = new BatchResource("test.plugin:webresources", "js", params, "");
        String url = resource.getUrl();
        BatchResource parsedResource = BatchResource.parse(url);

        assertEquals(resource.getType(), parsedResource.getType());
        assertEquals(resource.getModuleCompleteKey(), parsedResource.getModuleCompleteKey());
        assertEquals(resource.getParams(), parsedResource.getParams());
    }

    public void testEquals()
    {
        String moduleKey = "test.plugin:webresources";
        String type = "js";

        Map<String, String> params1 = new TreeMap<String, String>();
        params1.put("key", "value");
        params1.put("foo", "bar");
        BatchResource batch1 = new BatchResource(moduleKey, type, params1, "");

        Map<String, String> params2 = new TreeMap<String, String>();
        params2.put("key", "value");
        params2.put("foo", "bar");
        BatchResource batch2 = new BatchResource(moduleKey, type, params2, "");

        Map<String, String> params3 = new TreeMap<String, String>();
        params3.put("key", "value");
        params3.put("foo", "bart");
        BatchResource batch3 = new BatchResource(moduleKey, type, params3, "");

        assertEquals(batch1, batch2);
        assertNotSame(batch1, batch3);
    }
}
