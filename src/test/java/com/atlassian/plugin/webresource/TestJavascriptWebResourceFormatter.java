package com.atlassian.plugin.webresource;

import junit.framework.TestCase;

import java.util.HashMap;

public class TestJavascriptWebResourceFormatter extends TestCase
{
    private JavascriptWebResourceFormatter javascriptWebResourceFormatter;

    protected void setUp() throws Exception
    {
        super.setUp();
        javascriptWebResourceFormatter = new JavascriptWebResourceFormatter();
    }

    protected void tearDown() throws Exception
    {
        javascriptWebResourceFormatter = null;
        super.tearDown();
    }

    public void testMatches()
    {
        assertTrue(javascriptWebResourceFormatter.matches("blah.js"));
        assertFalse(javascriptWebResourceFormatter.matches("blah.css"));
    }

    public void testFormatResource()
    {
        final String resourceName = "atlassian.js";
        final String url = "/confluence/download/resources/confluence.web.resources:ajs/atlassian.js";
        assertEquals("<script type=\"text/javascript\" src=\"" + url + "\"></script>\n",
                     javascriptWebResourceFormatter.formatResource(resourceName, url, new HashMap()));
    }
}
