package com.atlassian.labs.plugins3.extender;

import com.atlassian.labs.plugins3.api.ApplicationInfo;
import com.atlassian.labs.plugins3.api.PluginDescriptorGenerator;
import com.atlassian.labs.plugins3.impl.DefaultPluginDescriptorGenerator;
import com.atlassian.labs.plugins3.spi.PluginDescriptorGeneratorFactory;
import com.atlassian.plugin.ModuleDescriptorFactory;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.osgi.factory.OsgiChainedModuleDescriptorFactoryCreator;
import com.atlassian.plugin.refimpl.ContainerManager;
import org.dom4j.Document;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 *
 */
public class DummyPluginDescriptorGeneratorFactory implements PluginDescriptorGeneratorFactory
{
    private final OsgiChainedModuleDescriptorFactoryCreator moduleDescriptorFactoryCreator;

    public DummyPluginDescriptorGeneratorFactory(final BundleContext bundleContext)
    {
        this.moduleDescriptorFactoryCreator = new OsgiChainedModuleDescriptorFactoryCreator(new OsgiChainedModuleDescriptorFactoryCreator.ServiceTrackerFactory()
        {
            public ServiceTracker create(String className)
            {
                ServiceTracker tracker = new ServiceTracker(bundleContext, className, null);
                tracker.open();
                return tracker;
            }
        });
    }

    public PluginDescriptorGenerator newInstance(Document doc)
    {
        return new DefaultPluginDescriptorGenerator(doc);
    }

    public ApplicationInfo getApplicationInfo()
    {
        return new DummyApplicationInfo();
    }

    public ModuleDescriptorFactory getModuleDescriptorFactory(final Plugin plugin)
    {
        return moduleDescriptorFactoryCreator.create(new OsgiChainedModuleDescriptorFactoryCreator.ResourceLocator()
        {
            public boolean doesResourceExist(String resourceName)
            {
                return plugin.getResource(resourceName) != null;
            }
        }, ContainerManager.getInstance().getModuleDescriptorFactory());
    }
}
