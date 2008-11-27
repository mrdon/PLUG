package com.atlassian.plugin.descriptors;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.ModuleDescriptorFactory;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;

import org.dom4j.Element;

/**
 * Utility class to create UnloadableModuleDescriptor instances when there are problems
 */
public final class UnrecognisedModuleDescriptorFactory
{
    /**
     * Creates a new UnrecognisedModuleDescriptor, for when a problem occurs during the retrieval
     * of the ModuleDescriptor itself.
     *
     * This instance has the same information as the original ModuleDescriptor, but also contains
     * an error message that reports the error.
     *
     * @param plugin the Plugin the ModuleDescriptor belongs to
     * @param element the XML Element used to construct the ModuleDescriptor
     * @param e the Throwable
     * @param moduleDescriptorFactory a ModuleDescriptorFactory used to retrieve ModuleDescriptor instances
     * @return a new UnloadableModuleDescriptor instance
     * @throws com.atlassian.plugin.PluginParseException if there was a problem constructing the UnloadableModuleDescriptor
     */
    public static <T, M extends ModuleDescriptor<T>> UnrecognisedModuleDescriptor<T> createUnrecognisedModuleDescriptor(final Plugin plugin, final Element element, final Throwable e, final ModuleDescriptorFactory<T, M> moduleDescriptorFactory) throws PluginParseException
    {
        final UnrecognisedModuleDescriptor<T> descriptor = new UnrecognisedModuleDescriptor<T>();
        descriptor.init(plugin, element);

        final String name = element.getName();
        final Class<M> moduleClass = moduleDescriptorFactory.getModuleDescriptorClass(name);
        String moduleClassName;

        if (moduleClass == null)
        {
            moduleClassName = descriptor.getKey();
        }
        else
        {
            moduleClassName = moduleClass.getName();
        }

        final String errorMsg = UnrecognisedModuleDescriptorFactory.constructErrorMessage(plugin, name, moduleClassName, e);

        descriptor.setErrorText(errorMsg);

        return descriptor;
    }

    /**
     * Constructs an error message from a module and exception
     *
     * @param plugin the Plugin the module belongs to
     * @param moduleName the name of the module
     * @param moduleClass the class of the module
     * @param e the Throwable
     * @return an appropriate String representing the error
     */
    private static String constructErrorMessage(final Plugin plugin, final String moduleName, final String moduleClass, final Throwable e)
    {
        String errorMsg;

        if ((e.getMessage() == null) || "".equals(e.getMessage()))
        {
            if (e instanceof PluginParseException)
            {
                errorMsg = "There was a problem loading the descriptor for module '" + moduleName + "' in plugin '" + (plugin == null ? "null" : plugin.getName()) + "'.\n ";
            }
            else if (e instanceof InstantiationException)
            {
                errorMsg = "Could not instantiate module descriptor: " + moduleClass + ".<br/>";
            }
            else if (e instanceof IllegalAccessException)
            {
                errorMsg = "Exception instantiating module descriptor: " + moduleClass + ".<br/>";
            }
            else if (e instanceof ClassNotFoundException)
            {
                errorMsg = "Could not find module descriptor class: " + moduleClass + ".<br/>";
            }
            else if (e instanceof NoClassDefFoundError)
            {
                errorMsg = "A required class was missing: " + moduleClass + ". Please check that you have all of the required dependencies.<br/>";
            }
            else
            {
                errorMsg = "There was a problem loading the module descriptor: " + moduleClass + ".<br/>";
            }
        }
        else
        {
            errorMsg = e.getMessage();
        }
        return errorMsg;
    }
}
