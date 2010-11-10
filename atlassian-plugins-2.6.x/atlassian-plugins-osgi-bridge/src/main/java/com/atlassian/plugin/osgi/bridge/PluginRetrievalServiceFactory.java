package com.atlassian.plugin.osgi.bridge;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.osgi.bridge.external.PluginRetrievalService;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

/**
 * Provides instances of {@link com.atlassian.plugin.osgi.bridge.external.PluginRetrievalService}, tailed for the consumer.
 *
 * @since 2.6.0
 */
public class PluginRetrievalServiceFactory implements ServiceFactory
{
    private final PluginAccessor pluginAccessor;

    public PluginRetrievalServiceFactory(PluginAccessor pluginAccessor)
    {
        this.pluginAccessor = pluginAccessor;
    }

    public Object getService(Bundle bundle, ServiceRegistration serviceRegistration)
    {
        return new PluginRetrievalServiceImpl(pluginAccessor, bundle);
    }

    public void ungetService(Bundle bundle, ServiceRegistration serviceRegistration, Object o)
    {
    }

    private static class PluginRetrievalServiceImpl implements PluginRetrievalService
    {
        private final Plugin plugin;

        public PluginRetrievalServiceImpl(PluginAccessor pluginAccessor, Bundle bundle)
        {
            String pluginKey = PluginBundleUtils.getPluginKey(bundle);
            plugin = pluginAccessor.getPlugin(pluginKey);
        }

        public Plugin getPlugin()
        {
            return plugin;
        }
    }
}
