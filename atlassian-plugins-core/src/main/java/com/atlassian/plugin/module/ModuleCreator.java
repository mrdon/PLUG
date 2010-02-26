package com.atlassian.plugin.module;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginParseException;

/**
 * A ModuleCreator is used when instantiating the module class object of a {@link com.atlassian.plugin.ModuleDescriptor}.
 * The ModuleCreator creates a bean for a given module class name and a plugin ModuleDescriptor.
 * Each ModuleCreator supports a specific prefix. Depending on the prefix the
 * {@link com.atlassian.plugin.module.ModuleClassFactory} will use a ModuleCreator to create the java bean.
 *
 * @since 2.5.0
 */
public interface ModuleCreator
{
    /**
     * Returns the prefix this ModuleCreator supports.
     *  
     * @return the prefix this ModuleCreator supports.
     */
    String getPrefix();

    /**
     * This method will create a java bean for the given name and ModuleDescriptor.
     *
     * @param name the name of the bean. Cannot contain a prefix. Cannot be null.
     * @param moduleDescriptor the ModuleDescriptor of the plugin. Cannot be null.
     * @return a new bean.
     * 
     * @throws PluginParseException Thrown if unable to parse the value or instantiate the bean name
     */
    <T> T createModule(String name, ModuleDescriptor<T> moduleDescriptor) throws PluginParseException;

    /**
     * Returns the class for this module.
     *
     * @param name the name of the bean. Cannot contain a prefix. Cannot be null.
     * @param moduleDescriptor the ModuleDescriptor of the plugin. Cannot be null.
     * @return the Class of the module.
     *
     * @throws ModuleClassNotFoundException Thrown if the bean class could not be found
     */
    Class getModuleClass(String name, final ModuleDescriptor moduleDescriptor) throws ModuleClassNotFoundException;

}
