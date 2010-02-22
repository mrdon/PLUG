package com.atlassian.plugin.osgi.module;

import com.atlassian.plugin.module.ModulePrefixProvider;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.osgi.spring.SpringContainerAccessor;

public class SpringBeanModulePrefixProvider implements ModulePrefixProvider
{
    private static final String SPRING_BEAN_PREFIX = "bean";

    public boolean supportsPrefix(String prefix)
    {
        return SPRING_BEAN_PREFIX.equals(prefix);
    }

    public <T> T create(String name, ModuleDescriptor<T> moduleDescriptor)
    {
        if (moduleDescriptor.getPlugin() instanceof SpringContainerAccessor)
        {
            return (T) ((SpringContainerAccessor)moduleDescriptor.getPlugin()).getBean(name);
        }
        else
        {
            throw new IllegalArgumentException("Failed to resolve '" + name + "'. You cannot use '"+ SPRING_BEAN_PREFIX +"' prefix with non-OSGi plugins");
        }
    }
}
