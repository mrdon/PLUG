package com.atlassian.plugin;

import com.atlassian.plugin.loaders.SinglePluginLoader;
import com.atlassian.plugin.util.ClassLoaderUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

public class DefaultModuleDescriptorFactory implements ModuleDescriptorFactory
{
    private static Log log = LogFactory.getLog(DefaultModuleDescriptorFactory.class);

    private Map<String,Class<? extends ModuleDescriptor>> moduleDescriptorClasses;
    private List<String> permittedModuleKeys = Collections.emptyList();

    public DefaultModuleDescriptorFactory()
    {
        this.moduleDescriptorClasses = new HashMap<String, Class<? extends ModuleDescriptor>>();
    }

    public Class<? extends ModuleDescriptor> getModuleDescriptorClass(String type)
    {
        return moduleDescriptorClasses.get(type);
    }

    public ModuleDescriptor getModuleDescriptor(String type) throws PluginParseException, IllegalAccessException, InstantiationException, ClassNotFoundException
    {
        if (shouldSkipModuleOfType(type))
            return null;

        Class moduleDescriptorClazz = getModuleDescriptorClass(type);

        if (moduleDescriptorClazz == null)
            throw new PluginParseException("Cannot find ModuleDescriptor class for plugin of type '" + type + "'.");

        return (ModuleDescriptor) ClassLoaderUtils.loadClass(moduleDescriptorClazz.getName(), SinglePluginLoader.class).newInstance();
    }

    protected boolean shouldSkipModuleOfType(String type)
    {
        return permittedModuleKeys != null && !permittedModuleKeys.isEmpty() && !permittedModuleKeys.contains(type);
    }

    public void setModuleDescriptors(Map<String, String> moduleDescriptorClassNames)
    {
        for (Iterator<Map.Entry<String,String>> it = moduleDescriptorClassNames.entrySet().iterator(); it.hasNext();)
        {
            Map.Entry<String,String> entry = it.next();
            Class<? extends ModuleDescriptor> descriptorClass = getClassFromEntry(entry);
            if (descriptorClass != null)
                addModuleDescriptor(entry.getKey(), descriptorClass);
        }
    }

    private Class<? extends ModuleDescriptor> getClassFromEntry(Map.Entry<String,String> entry)
    {
        if (shouldSkipModuleOfType(entry.getKey()))
            return null;

        Class<? extends ModuleDescriptor> descriptorClass = null;
        try
        {
            descriptorClass = (Class<? extends ModuleDescriptor>) ClassLoaderUtils.loadClass(entry.getValue(), getClass());

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

    public void addModuleDescriptor(String type, Class<? extends ModuleDescriptor> moduleDescriptorClass)
    {
        moduleDescriptorClasses.put(type, moduleDescriptorClass);
    }

    public void removeModuleDescriptorForType(String type)
    {
        moduleDescriptorClasses.remove(type);
    }

    protected Map<String, Class<? extends ModuleDescriptor>> getDescriptorClassesMap()
    {
        return moduleDescriptorClasses;
    }

    /**
     * Sets the list of module keys that will be loaded. If this list is empty, then the factory will
     * permit all recognised module types to load. This allows you to run the plugin system in a 'restricted mode'
     *
     * @param permittedModuleKeys List of (String) keys
     */
    public void setPermittedModuleKeys(List<String> permittedModuleKeys)
    {
        if (permittedModuleKeys == null)
            permittedModuleKeys = Collections.emptyList();

        this.permittedModuleKeys = permittedModuleKeys;
    }
}
