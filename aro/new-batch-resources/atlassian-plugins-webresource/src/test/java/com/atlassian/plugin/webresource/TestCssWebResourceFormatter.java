package com.atlassian.plugin.webresource;

import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

public class TestCssWebResourceFormatter extends TestCase
{
    private CssWebResourceFormatter cssWebResourceFormatter;

    protected void setUp() throws Exception
    {
        super.setUp();
        cssWebResourceFormatter = new CssWebResourceFormatter();
    }

    protected void tearDown() throws Exception
    {
        cssWebResourceFormatter = null;
        super.tearDown();
    }

    public void testMatches()
    {
        assertTrue(cssWebResourceFormatter.matches("blah.css"));
        assertFalse(cssWebResourceFormatter.matches("blah.js"));
    }

    public void testFormatResource()
    {
        final String resourceName = "master.css";
        final String url = "/confluence/download/resources/confluence.web.resources:master-styles/master.css";

        assertEquals("<link type=\"text/css\" rel=\"stylesheet\" href=\"" + url + "\" media=\"all\"/>\n",
                    cssWebResourceFormatter.formatResource(resourceName, url, new HashMap()));
    }

    public void testFormatResourceWithParameters()
    {
        final String resourceName = "master.css";
        final String url = "/confluence/download/resources/confluence.web.resources:master-styles/master.css";
        HashMap params = new HashMap();
        params.put("title", "Confluence Master CSS");
        params.put("charset", "utf-8");
        params.put("foo", "bar"); // test invalid parameter

        assertEquals("<link type=\"text/css\" rel=\"stylesheet\" href=\"" + url + "\" title=\"Confluence Master CSS\"" +
                    " charset=\"utf-8\" media=\"all\"/>\n",
                    cssWebResourceFormatter.formatResource(resourceName, url, params));

    }

    public void testFormatIEResource()
    {
        final String resourceName = "master-ie.css";
        final String url = "/confluence/download/resources/confluence.web.resources:master-styles/master-ie.css";

        Map params = new HashMap();
        params.put("ieonly", "true");
        params.put("media", "screen");
        assertEquals("<!--[if IE]>\n" +
                    "<link type=\"text/css\" rel=\"stylesheet\" href=\"" + url + "\" media=\"screen\"/>\n" +
                    "<![endif]-->\n",
                    cssWebResourceFormatter.formatResource(resourceName, url, params));
    }
}
