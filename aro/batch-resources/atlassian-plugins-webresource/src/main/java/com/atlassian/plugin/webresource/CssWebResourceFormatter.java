package com.atlassian.plugin.webresource;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.BooleanUtils;

import java.util.Map;
import java.util.List;
import java.util.Arrays;

public class CssWebResourceFormatter extends AbstractWebResourceFormatter
{
    private static final String CSS_EXTENSION = "css";
    private static final List<String> HANDLED_PARAMETERS = Arrays.asList("title", "media", "charset");

    public String getExtension()
    {
        return CSS_EXTENSION;
    }

    public boolean matches(String name)
    {
        return name != null && name.endsWith("." + CSS_EXTENSION);
    }
    
    public String formatResource(String url, Map params)
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append("<link type=\"text/css\" rel=\"stylesheet\" href=\"").append(url).append("\"");
        List attributes = getParametersAsAttributes(params);
        if(attributes!=null && attributes.size()>0)
        {
            buffer.append(" ").append(StringUtils.join(attributes.iterator(), " "));
        }

        // default media to all
        if(!params.containsKey("media"))
        {
            buffer.append(" media=\"all\"");
        }
        buffer.append("/>\n");

        // ie conditional commment
        if(BooleanUtils.toBoolean((String) params.get("ieonly")))
        {
            buffer.insert(0, "<!--[if IE]>\n");
            buffer.append("<![endif]-->\n");
        }

        return buffer.toString();
    }

    protected List<String> getAttributeParameters()
    {
        return HANDLED_PARAMETERS;
    }
}
