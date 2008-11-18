package com.atlassian.plugin.osgi.factory;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.ModuleDescriptorFactory;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.descriptors.UnrecognisedModuleDescriptor;
import org.apache.log4j.Logger;

/**
 * Module descriptor factory for deferred modules.  Turns every request for a module descriptor into a deferred
 * module so be sure that this factory is last in a list of factories.
 *
 * @since 2.1.2
 */
class UnrecognisedModuleDescriptorFallbackFactory implements ModuleDescriptorFactory
{
    private static final Logger log = Logger.getLogger(UnrecognisedModuleDescriptorFallbackFactory.class);
    public static final String DESCRIPTOR_TEXT = "Support for this module is not currently installed.";

    public ModuleDescriptor getModuleDescriptor(String type) throws PluginParseException, IllegalAccessException, InstantiationException, ClassNotFoundException
    {
        log.info("Unknown module descriptor of type "+type+" registered as a deferred descriptor.");
        UnrecognisedModuleDescriptor descriptor = new UnrecognisedModuleDescriptor();
        descriptor.setErrorText(DESCRIPTOR_TEXT);
        return descriptor;
    }

    public boolean hasModuleDescriptor(String type)
    {
        return true;
    }

    public Class<? extends ModuleDescriptor> getModuleDescriptorClass(String type)
    {
        return UnrecognisedModuleDescriptor.class;
    }
}
