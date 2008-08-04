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
    public static List<ResourceDescriptor> getResourceDescriptors(Element element) throws PluginParseException
    {
        return Resources.fromXml(element).getResourceDescriptors();
    }

    public static Map<String,String> getParams(Element element)
    {
        List<Element> elements = element.elements("param");

        Map<String,String> params = new HashMap<String,String>(elements.size());

        for (Element paramEl : elements)
        {
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
