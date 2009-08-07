package com.atlassian.plugin.webresource;

import org.apache.commons.lang.StringUtils;

import java.util.Map;
import java.util.List;
import java.util.Arrays;

public class JavascriptWebResource extends AbstractWebResourceFormatter
{
    static final JavascriptWebResource INSTANCE = new JavascriptWebResource();

    /**
     * An instance of a WebResourceFormatter that formats Javascript web resources
     * @since 2.4
     */
    public static final WebResourceFormatter FORMATTER_INSTANCE = INSTANCE;

    /**
     * An instance of a WebResourceFilter that filters Javascript web resources.
     * @since 2.4
     */
    public static final WebResourceFilter FILTER_INSTANCE = INSTANCE;

    private static final String JAVA_SCRIPT_EXTENSION = ".js";
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
        return buffer.toString();
    }

    protected List<String> getAttributeParameters()
    {
        return HANDLED_PARAMETERS;
    }
}