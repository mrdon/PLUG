package com.atlassian.plugin;

import com.atlassian.plugin.util.ClassLoaderUtils;
import com.atlassian.plugin.util.concurrent.CopyOnWriteMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class DefaultModuleDescriptorFactory<T, M extends ModuleDescriptor<T>> implements ModuleDescriptorFactory<T, M>
{
    private static Log log = LogFactory.getLog(DefaultModuleDescriptorFactory.class);

    private final Map<String, Class<M>> moduleDescriptorClasses = CopyOnWriteMap.newHashMap();
    private final List<String> permittedModuleKeys = new ArrayList<String>();

    public DefaultModuleDescriptorFactory()
    {}

    public Class<M> getModuleDescriptorClass(final String type)
    {
        return moduleDescriptorClasses.get(type);
    }

    public ModuleDescriptor<T> getModuleDescriptor(final String type) throws PluginParseException, IllegalAccessException, InstantiationException, ClassNotFoundException
    {
        if (shouldSkipModuleOfType(type))
        {
            return null;
        }

        final Class<M> moduleDescriptorClazz = getModuleDescriptorClass(type);

        if (moduleDescriptorClazz == null)
        {
            throw new PluginParseException("Cannot find ModuleDescriptor class for plugin of type '" + type + "'.");
        }

        return moduleDescriptorClazz.newInstance();
    }

    protected boolean shouldSkipModuleOfType(final String type)
    {
        synchronized (permittedModuleKeys)
        {
            return (permittedModuleKeys != null) && !permittedModuleKeys.isEmpty() && !permittedModuleKeys.contains(type);
        }
    }

    public void setModuleDescriptors(final Map<String, String> moduleDescriptorClassNames)
    {
        for (final Entry<String, String> entry : moduleDescriptorClassNames.entrySet())
        {
            final Class<M> descriptorClass = getClassFromEntry(entry);
            if (descriptorClass != null)
            {
                addModuleDescriptor(entry.getKey(), descriptorClass);
            }
        }
    }

    private Class<M> getClassFromEntry(final Map.Entry<String, String> entry)
    {
        if (shouldSkipModuleOfType(entry.getKey()))
        {
            return null;
        }

        try
        {
            @SuppressWarnings("unchecked")
            final Class<M> descriptorClass = (Class<M>) ClassLoaderUtils.loadClass(entry.getValue(), getClass());

            if (!ModuleDescriptor.class.isAssignableFrom(descriptorClass))
            {
                log.error("Configured plugin module descriptor class " + entry.getValue() + " does not inherit from ModuleDescriptor");
                return null;
            }
            return descriptorClass;
        }
        catch (final ClassNotFoundException e)
        {
            log.error("Unable to add configured plugin module descriptor " + entry.getKey() + ". Class not found: " + entry.getValue());
            return null;
        }
    }

    public boolean hasModuleDescriptor(final String type)
    {
        return moduleDescriptorClasses.containsKey(type);
    }

    public void addModuleDescriptor(final String type, final Class<M> moduleDescriptorClass)
    {
        moduleDescriptorClasses.put(type, moduleDescriptorClass);
    }

    public void removeModuleDescriptorForType(final String type)
    {
        moduleDescriptorClasses.remove(type);
    }

    protected Map<String, Class<M>> getDescriptorClassesMap()
    {
        return Collections.unmodifiableMap(moduleDescriptorClasses);
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
        {
            permittedModuleKeys = Collections.emptyList();
        }

        synchronized (this.permittedModuleKeys)
        {
            // synced
            this.permittedModuleKeys.clear();
            this.permittedModuleKeys.addAll(permittedModuleKeys);
        }
    }
}
