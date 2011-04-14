package com.atlassian.plugin.webresource;

import junit.framework.TestCase;
import org.apache.commons.lang.StringEscapeUtils;

import java.util.HashMap;
import java.util.Map;

public class TestCssWebResource extends TestCase
{
    private CssWebResource cssWebResource;

    protected void setUp() throws Exception
    {
        super.setUp();
        cssWebResource = new CssWebResource();
    }

    protected void tearDown() throws Exception
    {
        cssWebResource = null;
        super.tearDown();
    }

    public void testMatches()
    {
        assertTrue(cssWebResource.matches("blah.css"));
        assertFalse(cssWebResource.matches("blah.js"));
    }

    public void testFormatResource()
    {
        final String url = "/confluence/download/resources/confluence.web.resources:master-styles/master.css";

        assertEquals("<link type=\"text/css\" rel=\"stylesheet\" href=\"" + url + "\" media=\"all\">\n",
                    cssWebResource.formatResource(url, new HashMap<String, String>()));
    }

    // PLUG-753
    public void testUrlIsEscaped()
    {
        final String url = "/confluence/s/en\"><script>alert(document.cookie)</script>/2153/1/1/_/download/superbatch/css/batch.css";

        assertEquals("<link type=\"text/css\" rel=\"stylesheet\" href=\"" + StringEscapeUtils.escapeHtml(url) +
                "\" media=\"all\">\n", cssWebResource.formatResource(url, new HashMap<String, String>()));
    }

    public void testFormatResourceWithParameters()
    {
        final String url = "/confluence/download/resources/confluence.web.resources:master-styles/master.css";
        Map<String, String> params = new HashMap<String, String>();
        params.put("title", "Confluence Master CSS");
        params.put("charset", "utf-8");
        params.put("foo", "bar"); // test invalid parameter

        assertEquals("<link type=\"text/css\" rel=\"stylesheet\" href=\"" + url + "\" title=\"Confluence Master CSS\"" +
                    " charset=\"utf-8\" media=\"all\">\n",
                    cssWebResource.formatResource(url, params));
    }

    public void testFormatIEResource()
    {
        final String url = "/confluence/download/resources/confluence.web.resources:master-styles/master-ie.css";

        Map<String, String> params = new HashMap<String, String>();
        params.put("ieonly", "true");
        params.put("media", "screen");
        assertEquals("<!--[if IE]>\n" +
                    "<link type=\"text/css\" rel=\"stylesheet\" href=\"" + url + "\" media=\"screen\">\n" +
                    "<![endif]-->\n",
                    cssWebResource.formatResource(url, params));
    }

    public void testFormatConditionalResource()
    {
        final String url = "/confluence/download/resources/confluence.web.resources:master-styles/master-ie.css";

        Map<String, String> params = new HashMap<String, String>();
        params.put("conditionalComment", "IE");
        params.put("media", "screen");
        assertEquals("<!--[if IE]>\n" +
                    "<link type=\"text/css\" rel=\"stylesheet\" href=\"" + url + "\" media=\"screen\">\n" +
                    "<![endif]-->\n",
                    cssWebResource.formatResource(url, params));
    }

    public void testFormatConditionOverridesIEResource()
    {
        final String url = "/confluence/download/resources/confluence.web.resources:master-styles/master-ie.css";

        Map<String, String> params = new HashMap<String, String>();
        params.put("conditionalComment", "!IE");
        params.put("ieonly", "true");
        params.put("media", "screen");
        assertEquals("<!--[if !IE]>\n" +
                    "<link type=\"text/css\" rel=\"stylesheet\" href=\"" + url + "\" media=\"screen\">\n" +
                    "<![endif]-->\n",
                    cssWebResource.formatResource(url, params));
    }
}
