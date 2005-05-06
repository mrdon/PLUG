package com.atlassian.plugin;

import com.atlassian.plugin.elements.ResourceDescriptor;

import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.ArrayList;

import org.dom4j.Element;

public class Resources implements Resourced
{
    private List resourceDescriptors;

    public static Resources fromXml(Element element) throws PluginParseException
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

        return new Resources(templates);
    }

    public Resources(List resourceDescriptors)
    {
        this.resourceDescriptors = resourceDescriptors;
    }

   public List getResourceDescriptors()
    {
        return resourceDescriptors;
    }

    public List getResourceDescriptors(String type)
    {
        List typedResourceDescriptors = new LinkedList();

        for (Iterator iterator = resourceDescriptors.iterator(); iterator.hasNext();)
        {
            ResourceDescriptor resourceDescriptor = (ResourceDescriptor) iterator.next();
            if (resourceDescriptor.getType().equalsIgnoreCase(type))
            {
                typedResourceDescriptors.add(resourceDescriptor);
            }
        }

        return typedResourceDescriptors;
    }

    public ResourceDescriptor getResourceDescriptor(String type, String name)
    {
        for (Iterator iterator = resourceDescriptors.iterator(); iterator.hasNext();)
        {
            ResourceDescriptor resourceDescriptor = (ResourceDescriptor) iterator.next();
            if (resourceDescriptor.getType().equalsIgnoreCase(type) && resourceDescriptor.getName().equalsIgnoreCase(name))
            {
                return resourceDescriptor;
            }
        }

        return null;
    }
}
