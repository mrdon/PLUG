package com.atlassian.plugin.module;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.hostcontainer.HostContainer;

/**
 * TODO: Document this class / interface here
 */
public class ClassModulePrefixProvider implements ModulePrefixProvider
{
    protected final HostContainer hostContainer;

    public ClassModulePrefixProvider(final HostContainer hostContainer)
    {
        this.hostContainer = hostContainer;
    }

    public boolean supportsPrefix(final String prefix)
    {
        return "class".equals(prefix);
    }

    public <T> T create(String name, ModuleDescriptor<T> moduleDescriptor)
    {
        if (moduleDescriptor.getPlugin() instanceof ContainerManagedPlugin)
        {
            ContainerManagedPlugin cmPlugin = (ContainerManagedPlugin) moduleDescriptor.getPlugin();
            Class cls = null;
            try
            {
                cls = moduleDescriptor.getPlugin().loadClass(name, null);
            }
            catch (ClassNotFoundException e)
            {
                throw new RuntimeException(e);
            }
            return (T) cmPlugin.getContainerAccessor().createBean(cls);
        }
        else if (moduleDescriptor.getModuleClass() != null)
        {
            return hostContainer.create(moduleDescriptor.getModuleClass());
        }
        return null;
    }

}
