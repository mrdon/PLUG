package com.atlassian.plugin.descriptors;

import com.atlassian.core.util.ClassLoaderUtils;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.elements.ResourceDescriptor;
import com.atlassian.plugin.loaders.LoaderUtils;
import org.dom4j.Element;

import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;

public abstract class ResourcedModuleDescriptor extends AbstractModuleDescriptor
{
    List resourceDescriptors;

    public void init(Plugin plugin, Element element) throws PluginParseException
    {
        super.init(plugin, element);
        resourceDescriptors = LoaderUtils.getResourceDescriptors(element);
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
