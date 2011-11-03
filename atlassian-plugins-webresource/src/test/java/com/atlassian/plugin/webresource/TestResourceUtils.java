package com.atlassian.plugin.webresource;

import junit.framework.TestCase;

import java.util.LinkedHashMap;
import java.util.Map;

public class TestResourceUtils extends TestCase
{
    public void testGetType()
    {
        assertEquals("css", ResourceUtils.getType("/foo.css"));
        assertEquals("js", ResourceUtils.getType("/superbatch/js/foo.js"));
        assertEquals("", ResourceUtils.getType("/superbatch/js/foo."));
        assertEquals("", ResourceUtils.getType("/superbatch/js/foo"));
    }

    public void testCacheKeyParameterOrder() throws Exception
    {
        String path = "/A/year/is/a/long/time";
        Map<String,String> map = new LinkedHashMap<String, String> ();
        map.put(new String("Not"),"so");
        map.put(new String("long"),"Just");
        map.put(new String("once"),"around");
        map.put(new String("the"),"sun");

        Map<String,String> map2 = new LinkedHashMap<String, String> ();
        map2.put(new String("the"),"sun");
        map2.put(new String("once"),"around");
        map2.put(new String("long"),"Just");
        map2.put(new String("Not"),"so");

        assertEquals(ResourceUtils.buildCacheKey(path,map),ResourceUtils.buildCacheKey(path,map2));

        assertEquals(ResourceUtils.buildCacheKey(path,map).hashCode(),ResourceUtils.buildCacheKey(path,map2).hashCode());

    }
}
