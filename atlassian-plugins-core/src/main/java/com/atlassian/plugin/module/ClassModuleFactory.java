package com.atlassian.plugin.module;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.hostcontainer.HostContainer;

/**
 * The ClassModuleFactory creates a java bean for the given module class by using either the plugins container or the hostcontainer, depending
 * if the plugin implements {@link com.atlassian.plugin.module.ContainerManagedPlugin}.
 * The returned bean class should have all constructor dependencies injected. However it is the containers responsibility to inject the dependencies.
 *
 * The ClassModuleFactory expects the fully qualified name of the java class.
 *
 * @Since 2.5.0
 */
public class ClassModuleFactory implements ModuleFactory
{
    protected final HostContainer hostContainer;

    public static final String PREFIX = "class";

    public ClassModuleFactory(final HostContainer hostContainer)
    {
        this.hostContainer = hostContainer;
    }

    public <T> T createModule(String name, ModuleDescriptor<T> moduleDescriptor) throws PluginParseException
    {
        Class<T> cls = getModuleClass(name, moduleDescriptor);

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

    Class getModuleClass(final String name, final ModuleDescriptor moduleDescriptor) throws ModuleClassNotFoundException
    {
        try
        {
            return moduleDescriptor.getPlugin().loadClass(name, null);
        }
        catch (ClassNotFoundException e)
        {
            throw new ModuleClassNotFoundException(name, moduleDescriptor.getPluginKey(), moduleDescriptor.getKey(), e, createErrorMsg(name));
        }
    }

    private String createErrorMsg(String className)
    {
        StringBuilder builder = new StringBuilder();
        builder.append("Couldn't load the class '").append(className).append("'. ");
        builder.append("This could mean that you misspelled the name of the class (double check) or that ");
        builder.append("you're using a class in your plugin that you haven't provided bundle instructions for.");
        builder.append("See http://confluence.atlassian.com/x/QRS-Cg for more details on how to fix this.");
        return builder.toString();
    }

}
