package com.atlassian.plugin;

import com.atlassian.plugin.loaders.SinglePluginLoader;
import com.atlassian.plugin.util.ClassLoaderUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

public class DefaultModuleDescriptorFactory implements ModuleDescriptorFactory
{
    private static Log log = LogFactory.getLog(DefaultModuleDescriptorFactory.class);

    private Map moduleDescriptorClasses;
    private List permittedModuleKeys = Collections.EMPTY_LIST;

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
        if (permittedModuleKeys != null && !permittedModuleKeys.isEmpty() && !permittedModuleKeys.contains(type))
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
        // Skip excluded module descriptors
        if (permittedModuleKeys != null && !permittedModuleKeys.isEmpty() && !permittedModuleKeys.contains(entry.getKey()))
            return null;

        Class descriptorClass = null;
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
     * Sets the list of module keys that will be loaded. If this list is empty, then the factory will
     * permit all recognised module types to load. This allows you to run the plugin system in a 'restricted mode'
     *
     * @param permittedModuleKeys List of (String) keys
     */
    public void setPermittedModuleKeys(List permittedModuleKeys)
    {
        if (permittedModuleKeys == null)
            permittedModuleKeys = Collections.EMPTY_LIST;

        this.permittedModuleKeys = permittedModuleKeys;
    }
}
