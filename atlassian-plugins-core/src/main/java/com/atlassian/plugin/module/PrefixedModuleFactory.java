package com.atlassian.plugin.module;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginParseException;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * The default implementation of a {@link ModuleFactory}.
 * The module class name can contain a prefix and this prefix determines which {@link com.atlassian.plugin.module.ModuleFactory}
 * is used to create the java class for this module descriptor.
 *
 * IF no prefix is supplied it will use {@link ClassModuleFactory} to create the module class object.
 *
 * @Since 2.5.0
 */
public class PrefixedModuleFactory implements ModuleFactory
{
    Logger log = LoggerFactory.getLogger(PrefixedModuleFactory.class);
    private final Map<String, ModuleFactory> delegateModuleFactories;

    public PrefixedModuleFactory(Map<String, ModuleFactory> delegates)
    {
        this.delegateModuleFactories = delegates;
    }

    private ModuleFactory getModuleFactoryForPrefix(final ModuleReference moduleReference)
    {
        final ModuleFactory moduleFactory = delegateModuleFactories.get(moduleReference.prefix);
        if (moduleFactory == null)
        {
            throw new PluginParseException("Failed to create a module. Prefix '" + moduleReference.prefix+"' not supported");
        }
        return moduleFactory;
    }


    public <T> T createModule(String className, final ModuleDescriptor<T> moduleDescriptor) throws PluginParseException
    {
        Validate.notNull(className, "The className cannot be null");
        Validate.notNull(moduleDescriptor, "The moduleDescriptor cannot be null");

        final ModuleReference beanReference = getBeanReference(className);

        Object result = null;

        final ModuleFactory moduleFactory = getModuleFactoryForPrefix(beanReference);
        try
        {
            result = moduleFactory.createModule(beanReference.beanIdentifier, moduleDescriptor);
        }
        catch (NoClassDefFoundError error)
        {
            log.error("Detected an error (NoClassDefFoundError) instantiating the module for plugin '" + moduleDescriptor.getPlugin().getKey() + "'" +
                     " for module '" + moduleDescriptor.getKey() + "': " + error.getMessage() + ".  This error is usually caused by your" +
                     " plugin using a imported component class that itself relies on other packages in the product. You can probably fix this by" +
                     " adding the missing class's package to your <Import-Package> instructions; for more details on how to fix this, see" +
                     " http://confluence.atlassian.com/x/QRS-Cg .");
            throw error;
        }
        catch (LinkageError error)
        {
            log.error("Detected an error (LinkageError) instantiating the module for plugin '" + moduleDescriptor.getPlugin().getKey() + "'" +
                     " for module '" + moduleDescriptor.getKey() + "': " + error.getMessage() + ".  This error is usually caused by your" +
                     " plugin including copies of libraries in META-INF/lib unnecessarily. For more details on how to fix this, see" +
                     " http://confluence.atlassian.com/x/yQEhCw .");
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



    private ModuleReference getBeanReference(String className)
    {
        String prefix = ClassModuleFactory.PREFIX;
        final int prefixIndex = className.indexOf(":");
        if (prefixIndex != -1)
        {
            prefix = className.substring(0, prefixIndex);
            className = className.substring(prefixIndex + 1);
        }
        return new ModuleReference(prefix, className);
    }

    /**
     * This is not to be used.  It is only for backwards compatibility with old code that uses
     * {@link com.atlassian.plugin.PluginAccessor#getEnabledModulesByClass(Class)}.  This method can and will be
     * removed without warning.
     *
     * @param name The class name
     * @param moduleDescriptor The module descriptor
     * @param <T> The module class type
     * @return The module class
     * @throws ModuleClassNotFoundException
     * @deprecated Since 2.5.0
     */
    @Deprecated
    public <T> Class<T> guessModuleClass(final String name, final ModuleDescriptor<T> moduleDescriptor) throws ModuleClassNotFoundException
    {
        Validate.notNull(name, "The class name cannot be null");
        Validate.notNull(moduleDescriptor, "The module descriptor cannot be null");

        final ModuleReference moduleReference = getBeanReference(name);

        final ModuleFactory moduleFactory = getModuleFactoryForPrefix(moduleReference);
        Class<T> result = null;
        if (moduleFactory instanceof ClassModuleFactory)
        {
            result = ((ClassModuleFactory)moduleFactory).getModuleClass(moduleReference.beanIdentifier, moduleDescriptor);
        }

        return result;

    }

    private class ModuleReference
    {
        public String prefix;
        public String beanIdentifier;

        ModuleReference(String prefix, String beanIdentifier)
        {
            this.prefix = prefix;
            this.beanIdentifier = beanIdentifier;
        }
    }
}
