package com.atlassian.plugin.descriptors;

import com.atlassian.plugin.loaders.LoaderUtils;
import com.atlassian.plugin.PluginParseException;
import org.dom4j.Element;

import java.util.List;

public abstract class ResourcedModuleDescriptor extends AbstractModuleDescriptor
{
    List resourceDescriptors;

    public void init(Element element) throws PluginParseException
    {
        super.init(element);
        resourceDescriptors = LoaderUtils.getResourceDescriptors(element);
    }

    public List getResourceDescriptors()
    {
        return resourceDescriptors;
    }
}
