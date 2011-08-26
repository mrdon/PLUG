package com.atlassian.plugin.osgi.factory;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.ModuleDescriptorFactory;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.descriptors.UnrecognisedModuleDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Module descriptor factory for deferred modules.  Turns every request for a module descriptor into a deferred
 * module so be sure that this factory is last in a list of factories.
 *
 * @since 2.1.2
 * @see {@link UnrecognisedModuleDescriptor}
 */
class UnrecognisedModuleDescriptorFallbackFactory implements ModuleDescriptorFactory
{
    private static final Logger log = LoggerFactory.getLogger(UnrecognisedModuleDescriptorFallbackFactory.class);
    public static final String DESCRIPTOR_TEXT = "Support for this module is not currently installed.";

    public UnrecognisedModuleDescriptor getModuleDescriptor(final String type) throws PluginParseException, IllegalAccessException, InstantiationException, ClassNotFoundException
    {
        log.info("Unknown module descriptor of type " + type + " registered as an unrecognised descriptor.");
        final UnrecognisedModuleDescriptor descriptor = new UnrecognisedModuleDescriptor();
        descriptor.setErrorText(DESCRIPTOR_TEXT);
        return descriptor;
    }

    public boolean hasModuleDescriptor(final String type)
    {
        return true;
    }

    public Class<? extends ModuleDescriptor<?>> getModuleDescriptorClass(final String type)
    {
        return UnrecognisedModuleDescriptor.class;
    }
}
