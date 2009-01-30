package com.atlassian.plugin.osgi.bridge;

import org.springframework.osgi.context.event.OsgiBundleApplicationContextListener;
import org.springframework.osgi.context.event.OsgiBundleApplicationContextEvent;
import org.springframework.osgi.context.event.OsgiBundleContextFailedEvent;
import org.springframework.osgi.context.event.OsgiBundleContextRefreshedEvent;
import org.springframework.context.ApplicationContext;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.event.events.PluginContainerFailedEvent;
import com.atlassian.plugin.event.events.PluginContainerRefreshedEvent;

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
            pluginEventManager.broadcast(new PluginContainerFailedEvent<ApplicationContext>(
                    e.getApplicationContext(),
                    e.getBundle().getSymbolicName(),
                    e.getFailureCause()));
        }
        else if (evt instanceof OsgiBundleContextRefreshedEvent)
        {
            OsgiBundleContextRefreshedEvent e = (OsgiBundleContextRefreshedEvent)evt;
            pluginEventManager.broadcast(new PluginContainerRefreshedEvent<ApplicationContext>(
                    e.getApplicationContext(),
                    e.getBundle().getSymbolicName()));
        }
    }
}