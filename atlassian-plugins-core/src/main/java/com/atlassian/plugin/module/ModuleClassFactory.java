package com.atlassian.plugin.module;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginParseException;

/**
 * The {@link com.atlassian.plugin.module.ModuleClassFactory} creates the module class of a {@link com.atlassian.plugin.ModuleDescriptor}.
 * The ModuleClassFactory is injected into the {@link com.atlassian.plugin.descriptors.AbstractModuleDescriptor} and encapsulates the different
 * strategies how the module class can be created.
 *
 * @since 2.5.0
 */
public interface ModuleClassFactory
{
    /**
     * Creates the modules class. The module class name can contain a prefix. The delimiter of the prefix and the class name is ':'.
     * E.g.: 'bean:httpServletBean'. Which prefixes are supported depends on the registered {@link com.atlassian.plugin.module.ModuleCreator}.
     * The prefix is case in-sensitive.
     *
     * @param name              module class name, can contain a prefix followed by ":" and the class name. Cannot be null
     *                          If no prefix provided a default behaviour is assumed how to create the module class.
     *
     * @param moduleDescriptor  the {@link com.atlassian.plugin.ModuleDescriptor}. Cannot be null
     *
     * @return an instantiated object of the module class.
     *
     * @throws PluginParseException If it failed to create the object.
     */
    <T> T createModuleClass(String name, ModuleDescriptor<T> moduleDescriptor) throws PluginParseException;

    /**
     * Returns the module class. The module class name can contain a prefix. The delimiter of the prefix and the class name is ':'.
     * E.g.: 'bean:httpServletBean'. Which prefixes are supported depends on the registered {@link com.atlassian.plugin.module.ModuleCreator}.
     *
     * @param name               module class name, can contain a prefix followed by ":" and the class name. Cannot be null.
     *                           If no prefix provided a default behaviour is assumed how to create the module class.
     * @param moduleDescriptor   the {@link com.atlassian.plugin.ModuleDescriptor}. Cannot be null
     *
     * @return the module class.
     */
    <T> T getModuleClass(String name, ModuleDescriptor<T> moduleDescriptor);



    static final ModuleClassFactory NOOP_MODULE_CREATOR  = new NoOpModuleClassFactory();

    static class NoOpModuleClassFactory implements ModuleClassFactory
    {

        public <T> T createModuleClass(final String name, final ModuleDescriptor<T> moduleDescriptor)
                throws PluginParseException
        {
            return null;
        }

        public <T> T getModuleClass(final String name, final ModuleDescriptor<T> moduleDescriptor)
        {
            return null;
        }
    }
}
