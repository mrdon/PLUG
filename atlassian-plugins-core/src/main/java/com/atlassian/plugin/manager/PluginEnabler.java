package com.atlassian.plugin.manager;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginController;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.PluginState;
import com.atlassian.plugin.util.WaitUntil;

import java.util.*;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * Helper class that handles the problem of enabling a set of plugins at once.  This functionality is used for both
 * the initial plugin loading and manual plugin enabling.
 *
 * @since 2.2.0
 */
class PluginEnabler
{
    private final PluginAccessor pluginAccessor;
    private final PluginController pluginController;
    private Log log = LogFactory.getLog(PluginEnabler.class);

    public PluginEnabler(PluginAccessor pluginAccessor, PluginController pluginController)
    {
        this.pluginAccessor = pluginAccessor;
        this.pluginController = pluginController;
    }

    /**
     * Determines, recursively, which disabled plugins this plugin depends upon, and enables all of them at once.
     *
     * @param plugin The requested plugin to enable
     */
    void enableRecursively(Plugin plugin)
    {
        Set<String> dependentKeys = new HashSet<String>();
        scanDependencies(plugin, dependentKeys);

        List<Plugin> pluginsToEnable = new ArrayList<Plugin>();
        for (String key : dependentKeys)
        {
            pluginsToEnable.add(pluginAccessor.getPlugin(key));
        }
        enable(pluginsToEnable);
    }

    /**
     * Enables a collection of plugins at once, waiting for 60 seconds.  If any plugins are still in the enabling state,
     * the plugins are explicitly disabled.
     *
     * @param plugins The plugins to enable
     */
    void enable(Collection<Plugin> plugins)
    {

        final Set<Plugin> pluginsInEnablingState = new HashSet<Plugin>();
        for (final Plugin plugin : plugins)
        {
            try
            {
                plugin.enable();
                if (plugin.getPluginState() == PluginState.ENABLING)
                {
                    pluginsInEnablingState.add(plugin);
                }
            }
            catch (final RuntimeException ex)
            {
                log.error("Unable to enable plugin " + plugin.getKey(), ex);
            }
        }

        if (!pluginsInEnablingState.isEmpty())
        {
            // Now try to enable plugins that weren't enabled before, probably due to dependency ordering issues
            WaitUntil.invoke(new WaitUntil.WaitCondition()
            {
                public boolean isFinished()
                {
                    for (final Iterator<Plugin> i = pluginsInEnablingState.iterator(); i.hasNext();)
                    {
                        final Plugin plugin = i.next();
                        if (plugin.getPluginState() != PluginState.ENABLING)
                        {
                            i.remove();
                        }
                    }
                    return pluginsInEnablingState.isEmpty();
                }

                public String getWaitMessage()
                {
                    return "Plugins that have yet to enabled: " + pluginsInEnablingState;
                }
            }, 60);

            // Disable any plugins that aren't enabled by now
            if (!pluginsInEnablingState.isEmpty())
            {
                final StringBuilder sb = new StringBuilder();
                for (final Plugin plugin : pluginsInEnablingState)
                {
                    sb.append(plugin.getKey()).append(',');
                    pluginController.disablePlugin(plugin.getKey());
                }
                sb.deleteCharAt(sb.length() - 1);
                log.error("Unable to start the following plugins: " + sb.toString());
            }
        }
    }

    /**
     * Scans, recursively, to build a set of plugin dependencies for the target plugin
     *
     * @param plugin The plugin to scan
     * @param dependentKeys The set of keys collected so far
     */
    private void scanDependencies(Plugin plugin, Set<String> dependentKeys)
    {
        dependentKeys.add(plugin.getKey());
        
        // Ensure dependent plugins are enabled first
        for (String dependencyKey : plugin.getRequiredPlugins())
        {
            if (!dependentKeys.contains(dependencyKey) && !pluginAccessor.isPluginEnabled(dependencyKey))
            {
                scanDependencies(pluginAccessor.getPlugin(dependencyKey), dependentKeys);
            }
        }
    }
}