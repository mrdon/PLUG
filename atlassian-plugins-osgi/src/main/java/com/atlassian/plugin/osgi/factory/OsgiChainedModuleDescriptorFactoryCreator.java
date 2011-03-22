package com.atlassian.plugin.osgi.factory;

import com.atlassian.plugin.DefaultModuleDescriptorFactory;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.ModuleDescriptorFactory;
import com.atlassian.plugin.descriptors.ChainModuleDescriptorFactory;
import com.atlassian.plugin.hostcontainer.DefaultHostContainer;
import com.atlassian.plugin.osgi.external.ListableModuleDescriptorFactory;
import com.atlassian.plugin.osgi.factory.descriptor.ComponentImportModuleDescriptor;
import com.atlassian.plugin.osgi.factory.descriptor.ComponentModuleDescriptor;
import com.atlassian.plugin.osgi.factory.descriptor.ModuleTypeModuleDescriptor;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a {@link ModuleDescriptorFactory} suitable for building {@link ModuleDescriptor} instances in an OSGi context
 * for {@link OsgiPlugin} instances.
 * The result will be able to handle, in the following order:
 * <ul>
 * <li>OSGi-specific module descriptors like component, component-import, and module-type</li>
 * <li>Product-provided module descriptors</li>
 * <li>Any module descriptor factories exposed as OSGi services</li>
 * <li>Unknown module descriptors</li>
 * </ul>
 *
 * @since 2.7.0
 */
public class OsgiChainedModuleDescriptorFactoryCreator
{
    private final ServiceTrackerFactory serviceTrackerFactory;
    private final ModuleDescriptorFactory transformedDescriptorFactory = new DefaultModuleDescriptorFactory(new DefaultHostContainer())
    {{
        addModuleDescriptor("component", ComponentModuleDescriptor.class);
        addModuleDescriptor("component-import", ComponentImportModuleDescriptor.class);
        addModuleDescriptor("module-type", ModuleTypeModuleDescriptor.class);
    }};
    private static final Logger log = LoggerFactory.getLogger(OsgiChainedModuleDescriptorFactoryCreator.class);

    private volatile ServiceTracker moduleDescriptorFactoryTracker;

    public static interface ServiceTrackerFactory
    {

        ServiceTracker create(String className);
    }

    public static interface ResourceLocator
    {
        boolean doesResourceExist(String name);
    }

    public OsgiChainedModuleDescriptorFactoryCreator(ServiceTrackerFactory serviceTrackerFactory)
    {
        this.serviceTrackerFactory = serviceTrackerFactory;
    }

    public ModuleDescriptorFactory create(ResourceLocator resourceLocator, ModuleDescriptorFactory originalModuleDescriptorFactory)
    {
        // we really don't want two of these
        synchronized (this)
        {
            if (moduleDescriptorFactoryTracker == null)
            {
                moduleDescriptorFactoryTracker = serviceTrackerFactory.create(ModuleDescriptorFactory.class.getName());
            }
        }

        List<ModuleDescriptorFactory> factories = new ArrayList<ModuleDescriptorFactory>();

        factories.add(transformedDescriptorFactory);
        factories.add(originalModuleDescriptorFactory);
        Object[] serviceObjs = moduleDescriptorFactoryTracker.getServices();

        // Add all the dynamic module descriptor factories registered as osgi services
        if (serviceObjs != null)
        {
            for (Object fac : serviceObjs)
            {
                ModuleDescriptorFactory dynFactory = (ModuleDescriptorFactory) fac;

                // don't add Listable descriptor factories (module-type users) as the module type could get disabled
                // between here and when the plugin is actually looking for listable factories
                if (!(dynFactory instanceof ListableModuleDescriptorFactory))
                {
                    factories.add((ModuleDescriptorFactory) fac);
                }
            }
        }

        // Catch all unknown descriptors as unrecognised
        factories.add(new UnrecognisedModuleDescriptorFallbackFactory());

        return new ChainModuleDescriptorFactory(factories.toArray(new ModuleDescriptorFactory[factories.size()]));
    }
}
