package com.atlassian.plugin.descriptors;

import com.atlassian.plugin.loaders.LoaderUtils;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.elements.ResourceDescriptor;
import com.atlassian.core.util.ClassLoaderUtils;
import org.dom4j.Element;

import java.util.List;
import java.util.Iterator;

public abstract class ResourcedModuleDescriptor extends AbstractModuleDescriptor
{
    List resourceDescriptors;
    ResourceParameterGenerator parameterGenerator;

    public void init(Plugin plugin, Element element) throws PluginParseException
    {
        super.init(plugin, element);
        resourceDescriptors = LoaderUtils.getResourceDescriptors(element);

        if (getParams().containsKey("resource.parameter.generator"))
        {
            parameterGenerator = constructParameterGenerator((String)getParams().get("resource.parameter.generator"));
        }
    }

    protected ResourceParameterGenerator constructParameterGenerator(String clazz) throws PluginParseException
    {
        try
        {
            return (ResourceParameterGenerator) ClassLoaderUtils.loadClass(clazz, getClass()).newInstance();
        }
        catch (Exception e)
        {
            throw new PluginParseException("Error creating parameter generator class: " + clazz, e);
        }
    }

    public List getResourceDescriptors()
    {
        return resourceDescriptors;
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

    public ResourceParameterGenerator getParameterGenerator()
    {
        return parameterGenerator;
    }

    public boolean hasParameterGenerator()
    {
        return parameterGenerator != null;
    }
}
