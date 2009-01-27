package com.atlassian.plugin.osgi.bridge;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.springframework.osgi.context.event.OsgiBundleApplicationContextListener;
import com.atlassian.plugin.event.PluginEventManager;

public class BridgeActivator implements BundleActivator
{
    public void start(BundleContext bundleContext) throws Exception
    {
        // We can do this because the plugin event manager is a host component
        ServiceReference ref = bundleContext.getServiceReference(PluginEventManager.class.getName());
        if (ref == null)
        {
            throw new IllegalStateException("The PluginEventManager service must be exported from the application");
        }
        PluginEventManager pluginEventManager = (PluginEventManager) bundleContext.getService(ref);

        bundleContext.registerService(
                OsgiBundleApplicationContextListener.class.getName(),
                new SpringOsgiEventBridge(pluginEventManager),
                null);
    }

    public void stop(BundleContext bundleContext) throws Exception
    {
    }
}
