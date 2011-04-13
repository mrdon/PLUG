package com.atlassian.plugin.webresource;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class JavascriptWebResource extends AbstractWebResourceFormatter
{
    static final WebResourceFormatter FORMATTER = new JavascriptWebResource();

    private static final String JAVA_SCRIPT_EXTENSION = ".js";
    private static final String IEONLY_PARAM = "ieonly";
    private static final String CONDITION_PARAM = "conditionalComment";
    
    private static final List<String> HANDLED_PARAMETERS = Arrays.asList("charset");

    public boolean matches(String name)
    {
        return name != null && name.endsWith(JAVA_SCRIPT_EXTENSION);
    }

    public String formatResource(String url, Map<String, String> params)
    {
        StringBuffer buffer = new StringBuffer("<script type=\"text/javascript\" ");
        buffer.append("src=\"").append(url).append("\" ");
        buffer.append(StringUtils.join(getParametersAsAttributes(params).iterator(), " "));
        buffer.append("></script>\n");
        
        // ie conditional commment
        if (params.containsKey(CONDITION_PARAM))
        {
            String condition = params.get(CONDITION_PARAM);
            buffer.insert(0, "<!--[if " + condition + "]>\n");
            buffer.append("<![endif]-->\n");
        }
        else if (BooleanUtils.toBoolean(params.get(IEONLY_PARAM)))
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