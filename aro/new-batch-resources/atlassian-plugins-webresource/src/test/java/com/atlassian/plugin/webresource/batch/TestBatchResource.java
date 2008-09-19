package com.atlassian.plugin.webresource.batch;

import junit.framework.TestCase;

import java.util.Map;
import java.util.TreeMap;
import java.util.Collections;

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
        BatchResource resource = new BatchResource("js", "test.plugin:webresources", Collections.EMPTY_MAP);
        assertEquals("/download/batch/js/test.plugin:webresources/all.js", resource.getUrl());
    }

    public void testGetUrlWithParams()
    {
        Map<String, String> params = new TreeMap<String, String>();
        params.put("foo", "bar");
        params.put("moo", "cow");

        BatchResource resource = new BatchResource("js", "test.plugin:webresources", params);
        assertEquals("/download/batch/js/test.plugin:webresources/all.js?foo=bar&moo=cow", resource.getUrl());
    }

    public void testRoundTrip()
    {
        Map<String, String> params = new TreeMap<String, String>();
        params.put("foo", "bar");

        BatchResource resource = new BatchResource("js", "test.plugin:webresources", params);
        String url = resource.getUrl();
        BatchResource parsedResource = BatchResource.parse(url);

        assertEquals(resource.getType(), parsedResource.getType());
        assertEquals(resource.getModuleCompleteKey(), parsedResource.getModuleCompleteKey());
        assertEquals(resource.getParams(), parsedResource.getParams());
    }
}
