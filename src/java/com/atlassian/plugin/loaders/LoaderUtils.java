package com.atlassian.plugin.loaders;

import com.atlassian.plugin.elements.ResourceDescriptor;
import com.atlassian.plugin.PluginParseException;
import org.dom4j.Element;

import java.util.*;

public class LoaderUtils
{
    public static List getResourceDescriptors(Element element) throws PluginParseException
    {
        List elements = element.elements("resource");

        List templates = new ArrayList(elements.size());

        for (Iterator iterator = elements.iterator(); iterator.hasNext();)
        {
            final ResourceDescriptor resourceDescriptor = new ResourceDescriptor((Element) iterator.next());

            if (templates.contains(resourceDescriptor))
                throw new PluginParseException("Duplicate resource with type '" + resourceDescriptor.getType() + "' and name '" + resourceDescriptor.getName() + "' found");

            templates.add(resourceDescriptor);
        }

        return templates;
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
