package com.atlassian.plugin.osgi.factory;

import com.atlassian.plugin.ModuleDescriptor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Tracks module descriptors registered as services, then updates the descriptors map accordingly
 *
 * @since 2.2.0
 */
class ModuleDescriptorServiceTrackerCustomizer implements ServiceTrackerCustomizer
{
    private final Bundle bundle;
    private final OsgiPlugin plugin;
    private final Log log = LogFactory.getLog(ModuleDescriptorServiceTrackerCustomizer.class);

    public ModuleDescriptorServiceTrackerCustomizer(OsgiPlugin plugin)
    {
        this.bundle = plugin.getBundle();
        this.plugin = plugin;
    }

    public Object addingService(final ServiceReference serviceReference)
    {
        ModuleDescriptor<?> descriptor = null;
        if (serviceReference.getBundle() == bundle)
        {
            descriptor = (ModuleDescriptor<?>) bundle.getBundleContext().getService(serviceReference);
            plugin.addModuleDescriptor(descriptor);
            if (log.isInfoEnabled())
            {
                log.info("Dynamically registered new module descriptor: " + descriptor.getCompleteKey());
            }
        }
        return descriptor;
    }

    public void modifiedService(final ServiceReference serviceReference, final Object o)
    {
        if (serviceReference.getBundle() == bundle)
        {
            final ModuleDescriptor<?> descriptor = (ModuleDescriptor<?>) o;
            plugin.addModuleDescriptor(descriptor);
            if (log.isInfoEnabled())
            {
                log.info("Dynamically upgraded new module descriptor: " + descriptor.getCompleteKey());
            }
        }
    }

    public void removedService(final ServiceReference serviceReference, final Object o)
    {
        if (serviceReference.getBundle() == bundle)
        {
            final ModuleDescriptor<?> descriptor = (ModuleDescriptor<?>) o;
            plugin.clearModuleDescriptor(descriptor.getKey());
            if (log.isInfoEnabled())
            {
                log.info("Dynamically removed module descriptor: " + descriptor.getCompleteKey());
            }
        }
    }
}
