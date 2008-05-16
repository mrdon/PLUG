package com.atlassian.plugin.webresource;

import org.apache.commons.lang.StringUtils;

import java.util.Map;
import java.util.List;
import java.util.Arrays;

class JavascriptWebResourceFormatter extends AbstractWebResourceFormatter
{
    private static final String JAVA_SCRIPT_EXTENSION = ".js";
    private static final String JAVA_SCRIPT_MIN_EXTENSION = "-min.js";
    private static final List/*<String>*/ HANDLED_PARAMETERS = Arrays.asList(new String[] {"charset"});

    public boolean matches(String name)
    {
        return name != null && name.endsWith(JAVA_SCRIPT_EXTENSION);
    }

    public String formatResource(String name, String url, Map params)
    {
        StringBuffer buffer = new StringBuffer("<script type=\"text/javascript\" ");
        buffer.append("src=\"").append(url).append("\" ");
        buffer.append(StringUtils.join(getParametersAsAttributes(params).iterator(), " "));
        buffer.append("></script>\n");
        return buffer.toString();
    }

    public String minifyResourceLink(final String url)
    {
        String newUrl = url;
        if (url.endsWith(JAVA_SCRIPT_EXTENSION) && !url.endsWith(JAVA_SCRIPT_MIN_EXTENSION))
        {
            int lastDotPost = url.lastIndexOf(".");
            newUrl = url.substring(0, lastDotPost);
            newUrl += JAVA_SCRIPT_MIN_EXTENSION;
        }
        return newUrl;
    }

    protected List/*<String>*/ getAttributeParameters()
    {
        return HANDLED_PARAMETERS;
    }
}
