package com.atlassian.plugin.module;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginParseException;
import org.apache.commons.lang.Validate;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

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
    Logger log = LoggerFactory.getLogger(DefaultModuleClassFactory.class);
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
            throw new IllegalArgumentException("Module Creator with the prefix '" + moduleCreator.getPrefix() + "' is already registered.");
        }
    }

    public <T> T createModuleClass(String className, final ModuleDescriptor<T> moduleDescriptor) throws PluginParseException
    {
        Validate.notNull(className, "The className cannot be null");
        Validate.notNull(moduleDescriptor, "The moduleDescriptor cannot be null");

        final BeanReference beanReference = getBeanReference(className);

        Object result = null;

        final ModuleCreator moduleCreator = getModuleCreatorForPrefix(beanReference);
        try
        {
            result = moduleCreator.createBean(beanReference.beanIdentifier, moduleDescriptor);
        }
        catch (NoClassDefFoundError error)
        {
            log.error("Detected an error (NoClassDefFoundError) instantiating the module for plugin '" + moduleDescriptor.getPlugin().getKey() + "'" +
                     " for module '" + moduleDescriptor.getKey() + "': " + error.getMessage() + ".  This error is usually caused by your" +
                     " plugin using a imported component class that itself relies on other packages in the product. You can probably fix this by" +
                     " adding the missing class's package to your <Import-Package> instructions; for more details on how to fix this, see" +
                     " http://confluence.atlassian.com/x/QRS-Cg.");
            throw error;
        }
        catch (RuntimeException ex)
        {
            if (ex.getClass().getSimpleName().equals("UnsatisfiedDependencyException"))
            {
                log.error("Detected an error instantiating the module via Spring. This usually means that you haven't created a " +
                    "<component-import> for the interface you're trying to use. See http://confluence.atlassian.com/x/kgL3CQ " +
                    " for more details.");
            }
            throw ex;
        }

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

    public <T> T getModuleClass(final String name, final ModuleDescriptor<T> moduleDescriptor) throws ModuleClassNotFoundException
    {
        Validate.notNull(name, "The class name cannot be null");
        Validate.notNull(moduleDescriptor, "The module descriptor cannot be null");

        final BeanReference beanReference = getBeanReference(name);

        final ModuleCreator moduleCreator = getModuleCreatorForPrefix(beanReference);
        Object result = moduleCreator.getBeanClass(beanReference.beanIdentifier, moduleDescriptor);


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
