package com.atlassian.plugin.osgi.bridge;

import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.osgi.bridge.external.PluginRetrievalService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.springframework.osgi.context.event.OsgiBundleApplicationContextListener;

import java.util.Dictionary;
import java.util.Hashtable;

/**
 * Registers services for bridging Spring events with the plugin event system
 *
 * @since 2.2.0
 */
public class BridgeActivator implements BundleActivator
{
    public void start(BundleContext bundleContext) throws Exception
    {
        // We can do this because the plugin event manager is a host component
        PluginEventManager pluginEventManager = getHostComponent(bundleContext, PluginEventManager.class);

        // Register the listener for context refreshed and failed events
        bundleContext.registerService(
                OsgiBundleApplicationContextListener.class.getName(),
                new SpringOsgiEventBridge(pluginEventManager),
                null);

        // Register the listener for internal application context events like waiting for dependencies
        Dictionary<String, String> dict = new Hashtable<String, String>();
        dict.put("plugin-bridge", "true");
        bundleContext.registerService(
                OsgiBundleApplicationContextListener.class.getName(),
                new SpringContextEventBridge(pluginEventManager),
                dict);

        // Register the {@link PluginRetrievalService} service
        PluginAccessor pluginAccessor = getHostComponent(bundleContext, PluginAccessor.class);
        bundleContext.registerService(
                PluginRetrievalService.class.getName(),
                new PluginRetrievalServiceFactory(pluginAccessor),
                null);
    }

    private <T> T getHostComponent(BundleContext bundleContext, Class<T> componentClass)
    {
        ServiceReference ref = bundleContext.getServiceReference(componentClass.getName());
        if (ref == null)
        {
            throw new IllegalStateException("The " + componentClass.getName() + " service must be exported from the application");
        }
        return (T) bundleContext.getService(ref);
    }

    public void stop(BundleContext bundleContext) throws Exception
    {
    }
}
