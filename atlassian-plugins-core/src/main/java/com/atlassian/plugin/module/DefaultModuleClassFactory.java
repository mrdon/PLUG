package com.atlassian.plugin.module;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginParseException;
import org.apache.commons.lang.Validate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The default implementation of a {@link com.atlassian.plugin.module.ModuleClassFactory}.
 * The module class name can contain a prefix and this prefix determines which {@link com.atlassian.plugin.module.ModuleCreator}
 * is used to create the java class for this module descriptor.
 *
 * IF no prefix is supplied it will use {@link com.atlassian.plugin.module.ClassModuleCreator} to create the module class object.
 *
 * @Since 2.5.0
 */
public class DefaultModuleClassFactory implements ModuleClassFactory
{
    private final Map<String, ModuleCreator> moduleCreators = new HashMap<String, ModuleCreator>();

    public DefaultModuleClassFactory(List<ModuleCreator> creators)
    {
        for (ModuleCreator moduleCreator : creators)
        {
            registerModuleCreator(moduleCreator);
        }
    }

    public void registerModuleCreator(ModuleCreator moduleCreator)
    {
        final String prefix = moduleCreator.getPrefix();
        if (prefix == null)
        {
            throw new IllegalArgumentException("Module Creator cannot have a NULL prefix");
        }
        if (!moduleCreators.containsKey(prefix))
        {
            moduleCreators.put(moduleCreator.getPrefix(), moduleCreator);
        }
        else
        {
            throw new IllegalArgumentException("Module Creator with the prefix" + moduleCreator.getPrefix() + "' is already registered.");
        }
    }

    public <T> T createModuleClass(String className, final ModuleDescriptor<T> moduleDescriptor)
    {
        Validate.notNull(className, "The className cannot be null");
        Validate.notNull(moduleDescriptor, "The moduleDescriptor cannot be null");

        final BeanReference beanReference = getBeanReference(className);

        Object result = null;

        final ModuleCreator moduleCreator = getModuleCreatorForPrefix(beanReference);
        result = moduleCreator.createBean(beanReference.beanIdentifier, moduleDescriptor);

        if (result != null)
        {
            return (T) result;
        }
        else
        {
            throw new PluginParseException("Unable to create module instance from '" + className + "'");
        }
    }

    private ModuleCreator getModuleCreatorForPrefix(final BeanReference beanReference)
    {
        final ModuleCreator moduleCreator = moduleCreators.get(beanReference.prefix);
        if (moduleCreator == null)
        {
            throw new PluginParseException("Failed to create a module class. Prefix '" + beanReference.prefix+"' not supported");
        }
        return moduleCreator;
    }

    private BeanReference getBeanReference(String className)
    {
        String prefix = ClassModuleCreator.PREFIX;
        final int prefixIndex = className.indexOf(":");
        if (prefixIndex != -1)
        {
            prefix = className.substring(0, prefixIndex);
            className = className.substring(prefixIndex + 1);
        }
        return new BeanReference(prefix, className);
    }

    public <T> T getModuleClass(final String name, final ModuleDescriptor<T> moduleDescriptor)
    {
        Validate.notNull(name, "The class name cannot be null");
        Validate.notNull(moduleDescriptor, "The module descriptor cannot be null");

        final BeanReference beanReference = getBeanReference(name);

        Object result = null;

        final ModuleCreator moduleCreator = getModuleCreatorForPrefix(beanReference);
        result = moduleCreator.getBeanClass(beanReference.beanIdentifier, moduleDescriptor);

        if (result != null)
        {
            return (T) result;
        }
        else
        {
            throw new PluginParseException("Unable to create module class instance from '" + name + "'");
        }
    }

    private class BeanReference
    {
        public String prefix;
        public String beanIdentifier;

        BeanReference(String prefix, String beanIdentifier)
        {
            this.prefix = prefix;
            this.beanIdentifier = beanIdentifier;
        }
    }
}
