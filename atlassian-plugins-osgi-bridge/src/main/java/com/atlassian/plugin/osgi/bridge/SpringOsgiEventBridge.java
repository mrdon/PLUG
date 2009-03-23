package com.atlassian.plugin.osgi.bridge;

import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.event.events.PluginContainerFailedEvent;
import com.atlassian.plugin.event.events.PluginContainerRefreshedEvent;
import org.springframework.osgi.context.event.OsgiBundleApplicationContextEvent;
import org.springframework.osgi.context.event.OsgiBundleApplicationContextListener;
import org.springframework.osgi.context.event.OsgiBundleContextFailedEvent;
import org.springframework.osgi.context.event.OsgiBundleContextRefreshedEvent;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

import java.util.jar.Manifest;

/**
 * Bridges key Spring DM extender events with the plugin system
 *
 * @since 2.2.0
 */
public class SpringOsgiEventBridge implements OsgiBundleApplicationContextListener
{
    private final PluginEventManager pluginEventManager;

    public SpringOsgiEventBridge(PluginEventManager pluginEventManager)
    {
        this.pluginEventManager = pluginEventManager;
    }

    public void onOsgiApplicationEvent(OsgiBundleApplicationContextEvent evt)
    {
        if (evt instanceof OsgiBundleContextFailedEvent)
        {
            OsgiBundleContextFailedEvent e = (OsgiBundleContextFailedEvent)evt;
            pluginEventManager.broadcast(new PluginContainerFailedEvent(
                    e.getApplicationContext(),
                    getPluginKey(e.getBundle()),
                    e.getFailureCause()));
        }
        else if (evt instanceof OsgiBundleContextRefreshedEvent)
        {
            OsgiBundleContextRefreshedEvent e = (OsgiBundleContextRefreshedEvent)evt;
            pluginEventManager.broadcast(new PluginContainerRefreshedEvent(
                    e.getApplicationContext(),
                    getPluginKey(e.getBundle())));
        }
    }

    /**
     * Gets the plugin key from the bundle
     *
     * WARNING: shamelessly copied from {@link com.atlassian.plugin.osgi.util.OsgiHeaderUtil}, which can't be imported
     * due to creating a cyclic build dependency.  Ensure these two implementations are in sync.
     *
     * @param bundle The plugin bundle
     * @return The plugin key, cannot be null
     * @since 2.2.0
     */
    private static String getPluginKey(Bundle bundle)
    {
        return getPluginKey(
                bundle.getSymbolicName(),
                bundle.getHeaders().get("Atlassian-Plugin-Key"),
                bundle.getHeaders().get(Constants.BUNDLE_VERSION)
        );
    }

    private static String getPluginKey(Object bundleName, Object atlKey, Object version)
    {
        Object key = atlKey;
        if (key == null)
        {
            key = bundleName + "-" + version;
        }
        return key.toString();

    }
}
