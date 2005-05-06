package com.atlassian.plugin.loaders;

import com.atlassian.plugin.elements.ResourceDescriptor;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.Resources;
import org.dom4j.Element;

import java.util.*;

public class LoaderUtils
{
    /**
     * @deprecated use {@link com.atlassian.plugin.Resources#fromXml}
     */
    public static List getResourceDescriptors(Element element) throws PluginParseException
    {
        return Resources.fromXml(element).getResourceDescriptors();
    }

    public static Map getParams(Element element)
    {
        List elements = element.elements("param");

        Map params = new HashMap(elements.size());

        for (Iterator iterator = elements.iterator(); iterator.hasNext();)
        {
            Element paramEl = (Element) iterator.next();
            String name = paramEl.attributeValue("name");
            String value = paramEl.attributeValue("value");

            if (value == null && paramEl.getTextTrim() != null && !"".equals(paramEl.getTextTrim()))
            {
                value = paramEl.getTextTrim();
            }

            params.put(name, value);
        }

        return params;
    }
}
