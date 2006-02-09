package com.atlassian.plugin;

import com.atlassian.plugin.loaders.SinglePluginLoader;
import com.atlassian.plugin.util.ClassLoaderUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.List;

public class DefaultModuleDescriptorFactory implements ModuleDescriptorFactory
{
    private static Log log = LogFactory.getLog(DefaultModuleDescriptorFactory.class);

    private Map moduleDescriptorClasses;
    private List excludedModuleKeys;

    public DefaultModuleDescriptorFactory()
    {
        this.moduleDescriptorClasses = new HashMap();
    }

    public Class getModuleDescriptorClass(String type)
    {
        return (Class) moduleDescriptorClasses.get(type);
    }

    public ModuleDescriptor getModuleDescriptor(String type) throws PluginParseException, IllegalAccessException, InstantiationException, ClassNotFoundException
    {
        // When the key is in the excluded list, return null
        if (excludedModuleKeys != null && excludedModuleKeys.contains(type))
            return null;

        Class moduleDescriptorClazz = getModuleDescriptorClass(type);

        if (moduleDescriptorClazz == null)
            throw new PluginParseException("Cannot find ModuleDescriptor class for plugin of type '" + type + "'.");

        return (ModuleDescriptor) ClassLoaderUtils.loadClass(moduleDescriptorClazz.getName(), SinglePluginLoader.class).newInstance();
    }

    public void setModuleDescriptors(Map moduleDescriptorClassNames)
    {
        for (Iterator it = moduleDescriptorClassNames.entrySet().iterator(); it.hasNext();)
        {
            Map.Entry entry = (Map.Entry) it.next();
            Class descriptorClass = getClassFromEntry(entry);
            if (descriptorClass != null)
                addModuleDescriptor((String) entry.getKey(), descriptorClass);
        }
    }

    private Class getClassFromEntry(Map.Entry entry)
    {
        Class descriptorClass = null;

        // Skip excluded module descriptors
        if (excludedModuleKeys != null && excludedModuleKeys.contains(entry.getKey()))
            return null;

        try
        {
            descriptorClass = ClassLoaderUtils.loadClass((String) entry.getValue(), getClass());

            if (!ModuleDescriptor.class.isAssignableFrom(descriptorClass))
            {
                log.error("Configured plugin module descriptor class " + entry.getValue() + " does not inherit from ModuleDescriptor");
                descriptorClass = null;
            }
        }
        catch (ClassNotFoundException e)
        {
            log.error("Unable to add configured plugin module descriptor " + entry.getKey() + ". Class not found: " + entry.getValue());
        }

        return descriptorClass;
    }

    public boolean hasModuleDescriptor(String type)
    {
        return moduleDescriptorClasses.containsKey(type);
    }

    public void addModuleDescriptor(String type, Class moduleDescriptorClass)
    {
        moduleDescriptorClasses.put(type, moduleDescriptorClass);
    }

    public void removeModuleDescriptorForType(String type)
    {
        moduleDescriptorClasses.remove(type);
    }

    protected Map getDescriptorClassesMap()
    {
        return moduleDescriptorClasses;
    }

    /**
     * Retrieves a list of module descriptor that will not be loaded.
     *
     * @return List of (String) keys
     */
    public List getExcludedModuleKeys()
    {
        return excludedModuleKeys;
    }

    /**
     * Set the list of module descriptors that will not be loaded
     *
     * @param excludedModuleKeys List of (String) keys
     */
    public void setExcludedModuleKeys(List excludedModuleKeys)
    {
        this.excludedModuleKeys = excludedModuleKeys;
    }
}
