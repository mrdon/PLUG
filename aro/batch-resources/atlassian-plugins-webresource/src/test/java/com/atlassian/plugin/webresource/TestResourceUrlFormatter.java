package com.atlassian.plugin.webresource;

import junit.framework.TestCase;

public class TestResourceUrlFormatter extends TestCase
{
    private static final String TEST_MODULE_KEY = "atlassian.test.plugin:web-resources";

    public void testGetResourceUrl()
    {
        String url = ResourceUrlFormatter.getResourceUrl("/s/1/_", TEST_MODULE_KEY, "foo.css");
        assertEquals("/s/1/_/download/resources/atlassian.test.plugin:web-resources/foo.css", url);
    }

    public void testGetResourceUrlWithTrailingSlashPrefix()
    {
        String url = ResourceUrlFormatter.getResourceUrl("/s/1/_/", TEST_MODULE_KEY, "foo.css");
        assertEquals("/s/1/_/download/resources/atlassian.test.plugin:web-resources/foo.css", url);
    }

    public void testGetBatchResourceUrl()
    {
        String url = ResourceUrlFormatter.getBatchResourceUrl("/s/100/_", TEST_MODULE_KEY, "css");
        assertEquals("/s/100/_/download/resources/css/atlassian.test.plugin:web-resources", url);
    }

    public void testGetBatchResourceUrlWithTrailingSlashPrefix()
    {
        String url = ResourceUrlFormatter.getBatchResourceUrl("/s/100/_/", TEST_MODULE_KEY, "js");
        assertEquals("/s/100/_/download/resources/js/atlassian.test.plugin:web-resources", url);        
    }
}
