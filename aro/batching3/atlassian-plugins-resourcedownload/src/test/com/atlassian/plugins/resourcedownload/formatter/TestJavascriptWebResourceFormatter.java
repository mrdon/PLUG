package com.atlassian.plugins.resourcedownload.formatter;

import junit.framework.TestCase;
import junit.framework.Assert;

import java.util.HashMap;

import com.atlassian.plugin.resourcedownload.formatter.JavascriptWebResourceFormatter;

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
        final String url = "/confluence/download/resources/confluence.web.resources:ajs/atlassian.js";
        Assert.assertEquals("<script type=\"text/javascript\" src=\"" + url + "\" ></script>\n",
                     javascriptWebResourceFormatter.formatResource(url, new HashMap()));
    }
}