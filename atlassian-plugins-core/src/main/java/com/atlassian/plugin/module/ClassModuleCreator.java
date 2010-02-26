package com.atlassian.plugin.module;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.hostcontainer.HostContainer;

/**
 * The ClassModuleCreator creates a java bean for the given module class by using either the plugins container or the hostcontainer, depending
 * if the plugin implements {@link com.atlassian.plugin.module.ContainerManagedPlugin}.
 * The returned bean class should have all constructor dependencies injected. However it is the containers responsibility to inject the dependencies.
 *
 * The ClassModuleCreator expects the fully qualified name of the java class.
 *
 * @Since 2.5.0
 */
public class ClassModuleCreator implements ModuleCreator
{
    protected final HostContainer hostContainer;
    public static final String PREFIX = "class";

    public ClassModuleCreator(final HostContainer hostContainer)
    {
        this.hostContainer = hostContainer;
    }

    public String getPrefix()
    {
        return PREFIX;
    }

    public <T> T createBean(String name, ModuleDescriptor<T> moduleDescriptor) throws PluginParseException
    {
        Class<T> cls = moduleDescriptor.getModuleClass();
        if (cls == null)
        {
            cls = getBeanClass(name, moduleDescriptor);
        }
        
        if (moduleDescriptor.getPlugin() instanceof ContainerManagedPlugin)
        {
            ContainerManagedPlugin cmPlugin = (ContainerManagedPlugin) moduleDescriptor.getPlugin();
            return cmPlugin.getContainerAccessor().createBean(cls);
        }
        else if (cls != null)
        {
            return hostContainer.create(cls);
        }
        return null;
    }

    public Class getBeanClass(final String name, final ModuleDescriptor moduleDescriptor) throws ModuleClassNotFoundException
    {
        try
        {
            return moduleDescriptor.getPlugin().loadClass(name, null);
        }
        catch (ClassNotFoundException e)
        {
            throw new ModuleClassNotFoundException(name, moduleDescriptor.getPluginKey(), moduleDescriptor.getKey(), e);
        }
    }

}
