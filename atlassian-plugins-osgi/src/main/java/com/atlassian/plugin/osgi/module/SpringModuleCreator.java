package com.atlassian.plugin.osgi.module;

import com.atlassian.plugin.module.ContainerManagedPlugin;
import com.atlassian.plugin.module.ModuleClassNotFoundException;
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

    public <T> T createModule(String name, ModuleDescriptor<T> moduleDescriptor)
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

    public Class getModuleClass(final String name, final ModuleDescriptor moduleDescriptor)
    {
        if (moduleDescriptor.getPlugin() instanceof ContainerManagedPlugin)
        {
            ContainerManagedPlugin containerManagedPlugin = (ContainerManagedPlugin) moduleDescriptor.getPlugin();
            try
            {
                final Object beanObj = ((SpringContainerAccessor) containerManagedPlugin.getContainerAccessor()).getBean(name);
                return beanObj.getClass();
            }
            catch (Exception ex)
            {
                throw new ModuleClassNotFoundException(name, moduleDescriptor.getPluginKey(), moduleDescriptor.getKey(), ex, createErrorMsg(name));
            }

        }
        else
        {
            throw new IllegalArgumentException("Failed to resolve '" + name + "'. You cannot use '" + PREFIX + "' prefix with non-OSGi plugins");
        }
    }

    private final String createErrorMsg(String springBeanId)
    {
        StringBuilder builder = new StringBuilder();
        builder.append("Couldn't find the spring bean reference with the id '").append(springBeanId).append("'. ");
        builder.append("Please make sure you have defined a spring bean with this id within this plugin. Either using a native spring configuration or the component module descriptor, the spring bean id is the key of the module descriptor.");
        builder.append("If the spring bean you refer to is not part of this plugin, please make sure it is declared as public so it is visible to other plugins.");
        return builder.toString();
    }
}
