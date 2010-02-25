package com.atlassian.plugin.osgi.module;

import com.atlassian.plugin.module.ContainerManagedPlugin;
import com.atlassian.plugin.module.ModuleCreator;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.osgi.spring.SpringContainerAccessor;

/**
 * The SpringModuleCreator creates a java bean for the given module class by resolving the name to spring bean reference.
 * It returns a reference to this bean.
 * 
 * @since 2.5.0
 */
public class SpringModuleCreator implements ModuleCreator
{
    private static final String PREFIX = "bean";

    public String getPrefix()
    {
        return PREFIX;
    }

    public <T> T createBean(String name, ModuleDescriptor<T> moduleDescriptor)
    {
        if (moduleDescriptor.getPlugin() instanceof ContainerManagedPlugin)
        {
            ContainerManagedPlugin containerManagedPlugin = (ContainerManagedPlugin) moduleDescriptor.getPlugin();
            return (T) ((SpringContainerAccessor) containerManagedPlugin.getContainerAccessor()).getBean(name);
        }
        else
        {
            throw new IllegalArgumentException("Failed to resolve '" + name + "'. You cannot use '"+ PREFIX +"' prefix with non-OSGi plugins");
        }
    }

    public Class getBeanClass(final String name, final ModuleDescriptor moduleDescriptor)
    {
        if (moduleDescriptor.getPlugin() instanceof ContainerManagedPlugin)
        {
            ContainerManagedPlugin containerManagedPlugin = (ContainerManagedPlugin) moduleDescriptor.getPlugin();
            final Object beanObj = ((SpringContainerAccessor) containerManagedPlugin.getContainerAccessor()).getBean(name);
            return beanObj.getClass();
        }
        else
        {
            throw new IllegalArgumentException("Failed to resolve '" + name + "'. You cannot use '"+ PREFIX +"' prefix with non-OSGi plugins");
        }
    }
}
