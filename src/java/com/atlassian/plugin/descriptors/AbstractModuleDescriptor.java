package com.atlassian.plugin.descriptors;

import com.atlassian.core.util.ClassLoaderUtils;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.loaders.LoaderUtils;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.loaders.LoaderUtils;
import org.dom4j.Element;

import java.util.Map;

public abstract class AbstractModuleDescriptor implements ModuleDescriptor
{
    private Plugin plugin;
    String key;
    String name;
    Class moduleClass;
    String description;
    boolean enabledByDefault = true;
    Map params;

    public void init(Plugin plugin, Element element) throws PluginParseException
    {
        this.plugin = plugin;
        this.key = element.attributeValue("key");
        this.name = element.attributeValue("name");

        String clazz = element.attributeValue("class");
        try
        {
            moduleClass = ClassLoaderUtils.loadClass(clazz, getClass());
        }
        catch (ClassNotFoundException e)
        {
            throw new PluginParseException("Could not load class: " + clazz);
        }

        this.description = element.elementTextTrim("description");
        params = LoaderUtils.getParams(element);

        if ("disabled".equalsIgnoreCase(element.attributeValue("state")))
            enabledByDefault = false;
    }

    public boolean isEnabledByDefault()
    {
        return enabledByDefault;
    }

    /**
     * Check that the module class of this descriptor implements a given interface, or extends a given class.
     * @param requiredModuleClazz The class this module's class must implement or extend.
     * @throws PluginParseException If the module class does not implement or extend the given class.
     */
    final protected void assertModuleClassImplements(Class requiredModuleClazz) throws PluginParseException
    {
        if (!requiredModuleClazz.isAssignableFrom(getModuleClass()))
            throw new PluginParseException("Given module class: " + getModuleClass().getName() + " does not implement " + requiredModuleClazz.getName());
    }

    public String getCompleteKey() {
        return plugin.getKey() + ":" + getKey();
    }

    public String getKey()
    {
        return key;
    }

    public String getName()
    {
        return name;
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

    public Map getParams()
    {
        return params;
    }
}
