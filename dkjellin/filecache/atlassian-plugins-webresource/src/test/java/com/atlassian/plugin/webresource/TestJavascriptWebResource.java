package com.atlassian.plugin.webresource;

import junit.framework.TestCase;
import org.apache.commons.lang.StringEscapeUtils;

import java.util.HashMap;
import java.util.Map;

public class TestJavascriptWebResource extends TestCase
{
    private JavascriptWebResource javascriptWebResource;

    protected void setUp() throws Exception
    {
        super.setUp();
        javascriptWebResource = new JavascriptWebResource();
    }

    protected void tearDown() throws Exception
    {
        javascriptWebResource = null;
        super.tearDown();
    }

    public void testMatches()
    {
        assertTrue(javascriptWebResource.matches("blah.js"));
        assertFalse(javascriptWebResource.matches("blah.css"));
    }

    public void testFormatResource()
    {
        final String url = "/confluence/download/resources/confluence.web.resources:ajs/atlassian.js";
        assertEquals("<script type=\"text/javascript\" src=\"" + url + "\" ></script>\n",
                     javascriptWebResource.formatResource(url, new HashMap<String, String>()));
    }

    // PLUG-753
    public void testUrlIsEscaped()
    {
        final String url = "/confluence/s/en\"><script>alert(document.cookie)</script>/2153/1/1/_/download/superbatch/js/batch.js";

        assertEquals("<script type=\"text/javascript\" src=\"" + StringEscapeUtils.escapeHtml(url) +
                "\" ></script>\n", javascriptWebResource.formatResource(url, new HashMap<String, String>()));
    }

    public void testFormatIEResource()
    {
        final String url = "/confluence/download/resources/confluence.web.resources:ajs/atlassian-ie.js";

        Map<String, String> params = new HashMap<String, String>();
        params.put("ieonly", "true");
        params.put("media", "screen");
        assertEquals("<!--[if IE]>\n" +
                     "<script type=\"text/javascript\" src=\"" + url + "\" ></script>\n" +
                     "<![endif]-->\n",
                     javascriptWebResource.formatResource(url, params));
    }

    public void testFormatConditionalResource()
    {
        final String url = "/confluence/download/resources/confluence.web.resources:ajs/atlassian-ie.js";

        Map<String, String> params = new HashMap<String, String>();
        params.put("conditionalComment", "IE");
        params.put("media", "screen");
        assertEquals("<!--[if IE]>\n" +
                     "<script type=\"text/javascript\" src=\"" + url + "\" ></script>\n" +
                     "<![endif]-->\n",
                     javascriptWebResource.formatResource(url, params));
    }

    public void testFormatConditionOverridesIEResource()
    {
        final String url = "/confluence/download/resources/confluence.web.resources:ajs/atlassian-ie.js";

        Map<String, String> params = new HashMap<String, String>();
        params.put("conditionalComment", "!IE");
        params.put("ieonly", "true");
        params.put("media", "screen");
        assertEquals("<!--[if !IE]>\n" +
                    "<script type=\"text/javascript\" src=\"" + url + "\" ></script>\n" +
                    "<![endif]-->\n",
                    javascriptWebResource.formatResource(url, params));
    }
}
