package com.atlassian.plugin.descriptors;

import com.atlassian.core.util.ClassLoaderUtils;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.loaders.LoaderUtils;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.loaders.LoaderUtils;
import org.dom4j.Element;

import java.util.Map;

public abstract class AbstractModuleDescriptor implements ModuleDescriptor
{
    String key;
    String name;
    Class moduleClass;
    String description;
    Map params;

    public void init(Element element) throws PluginParseException
    {
        setKey(element.attributeValue("key"));
        setName(element.attributeValue("name"));

        String clazz = element.attributeValue("class");
        try
        {
            moduleClass = ClassLoaderUtils.loadClass(clazz, getClass());
        }
        catch (ClassNotFoundException e)
        {
            throw new PluginParseException("Could not load class: " + clazz);
        }

        setDescription(element.elementTextTrim("description"));
        params = LoaderUtils.getParams(element);
    }

    public String getKey()
    {
        return key;
    }

    public void setKey(String key)
    {
        this.key = key;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public Class getModuleClass()
    {
        return moduleClass;
    }

    public abstract Object getModule();

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }


    public Map getParams()
    {
        return params;
    }
}
