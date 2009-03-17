package com.atlassian.plugin.osgi.factory;

import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Bundle;
import org.dom4j.Element;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.atlassian.plugin.osgi.external.ListableModuleDescriptorFactory;
import com.atlassian.plugin.descriptors.UnrecognisedModuleDescriptor;
import com.atlassian.plugin.ModuleDescriptor;

import java.util.List;
import java.util.ArrayList;

/**
 * Service tracker that tracks {@link com.atlassian.plugin.osgi.external.ListableModuleDescriptorFactory} instances and handles transforming
 * {@link com.atlassian.plugin.descriptors.UnrecognisedModuleDescriptor}} instances into modules if the new factory supports them.  Updates to factories
 * and removal are also handled.
 *
 * @since 2.1.2
 */
class UnrecognizedModuleDescriptorServiceTrackerCustomizer implements ServiceTrackerCustomizer
{

    private final Bundle bundle;
    private final OsgiPlugin plugin;
    private final Log log = LogFactory.getLog(UnrecognizedModuleDescriptorServiceTrackerCustomizer.class);

    public UnrecognizedModuleDescriptorServiceTrackerCustomizer(OsgiPlugin plugin)
    {
        this.bundle = plugin.getBundle();
        this.plugin = plugin;
    }
    /**
     * Turns any {@link com.atlassian.plugin.descriptors.UnrecognisedModuleDescriptor} modules that can be handled by the new factory into real
     * modules
     */
    public Object addingService(final ServiceReference serviceReference)
    {
        final ListableModuleDescriptorFactory factory = (ListableModuleDescriptorFactory) bundle.getBundleContext().getService(serviceReference);
        for (final UnrecognisedModuleDescriptor unrecognised : getModuleDescriptorsByDescriptorClass(UnrecognisedModuleDescriptor.class))
        {
            final Element source = plugin.getModuleElements().get(unrecognised.getKey());
            if ((source != null) && factory.hasModuleDescriptor(source.getName()))
            {
                try
                {
                    final ModuleDescriptor<?> descriptor = factory.getModuleDescriptor(source.getName());
                    descriptor.init(unrecognised.getPlugin(), source);
                    plugin.addModuleDescriptor(descriptor);
                    if (log.isInfoEnabled())
                    {
                        log.info("Turned plugin module " + descriptor.getCompleteKey() + " into module " + descriptor);
                    }
                }
                catch (final Exception e)
                {
                    log.error("Unable to transform " + unrecognised.getCompleteKey() + " into actual plugin module using factory " + factory, e);
                    unrecognised.setErrorText(e.getMessage());
                }
            }
        }
        return factory;
    }

    /**
     * Updates any local module descriptors that were created from the modified factory
     */
    public void modifiedService(final ServiceReference serviceReference, final Object o)
    {
        removedService(serviceReference, o);
        addingService(serviceReference);
    }

    /**
     * Reverts any current module descriptors that were provided from the factory being removed into {@link
     * UnrecognisedModuleDescriptor} instances.
     */
    public void removedService(final ServiceReference serviceReference, final Object o)
    {
        final ListableModuleDescriptorFactory factory = (ListableModuleDescriptorFactory) o;
        for (final Class<ModuleDescriptor<?>> moduleDescriptorClass : factory.getModuleDescriptorClasses())
        {
            for (final ModuleDescriptor<?> descriptor : getModuleDescriptorsByDescriptorClass(moduleDescriptorClass))
            {
                final UnrecognisedModuleDescriptor unrecognisedModuleDescriptor = new UnrecognisedModuleDescriptor();
                final Element source = plugin.getModuleElements().get(descriptor.getKey());
                if (source != null)
                {
                    unrecognisedModuleDescriptor.init(plugin, source);
                    unrecognisedModuleDescriptor.setErrorText(UnrecognisedModuleDescriptorFallbackFactory.DESCRIPTOR_TEXT);
                    plugin.addModuleDescriptor(unrecognisedModuleDescriptor);
                    if (log.isInfoEnabled())
                    {
                        log.info("Removed plugin module " + unrecognisedModuleDescriptor.getCompleteKey() + " as its factory was uninstalled");
                    }
                }
            }
        }
    }

    /**
     *
     * @param descriptor
     * @param <T>
     * @return
     */
    <T extends ModuleDescriptor<?>> List<T> getModuleDescriptorsByDescriptorClass(final Class<T> descriptor)
    {
        final List<T> result = new ArrayList<T>();

        for (final ModuleDescriptor<?> moduleDescriptor : plugin.getModuleDescriptors())
        {
            if (moduleDescriptor.getClass()
                    .isAssignableFrom(descriptor))
            {
                result.add(descriptor.cast(moduleDescriptor));
            }
        }
        return result;
    }
}