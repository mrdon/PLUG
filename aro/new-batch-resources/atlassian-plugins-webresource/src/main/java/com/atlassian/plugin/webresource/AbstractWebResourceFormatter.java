package com.atlassian.plugin.webresource;

import org.apache.commons.lang.StringUtils;

import java.util.Map;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

abstract class AbstractWebResourceFormatter implements WebResourceFormatter
{
    /**
     * Should return a List of parameter name {@link String}s, which the WebResourceFormatter will write out as
     * HTML attributes.
     * 
     * @return a {@link List} of parameter names
     */
    protected abstract List<String> getAttributeParameters();

    /**
     * A convenient method to convert the given parameter map into a List of HTML {@link String} attributes.
     * For example, an entry with key 'foo' and value 'bar' is converted to the attribute string, foo="bar".
     * <p>
     * Only parameters that are supported by the WebResourceFormatter are formatted (See {@link #getAttributeParameters()}).
     * @param params a {@link Map} of parameters
     * @return a list of HTML {@link String} attributes
     */
    protected List getParametersAsAttributes(Map params)
    {
        final List attributes = new ArrayList();
        for (Iterator iterator = params.entrySet().iterator(); iterator.hasNext();)
        {
            Map.Entry entry = (Map.Entry) iterator.next();
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            if(StringUtils.isNotBlank(key) && getAttributeParameters().contains(key.toLowerCase()))
            {
                attributes.add(key + "=\"" + value + "\"");
            }
        }
        return attributes;
    }
}
