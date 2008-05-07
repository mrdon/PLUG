package com.atlassian.plugin.classloader;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.cache.Cache;
import com.atlassian.cache.memory.MemoryCache;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.URL;
import java.util.Collection;
import java.util.Iterator;

/**
 *
 */
public class PluginsClassLoader extends AbstractClassLoader
{
    private static final Log log = LogFactory.getLog(PluginsClassLoader.class);
    private final PluginAccessor pluginAccessor;
    private final Cache/*<String,Plugin>*/ pluginResourceIndex = new MemoryCache(this.getClass().getName() + "#ResourceIndex");
    private final Cache/*<String,Plugin>*/ pluginClassIndex = new MemoryCache(this.getClass().getName() + "#ClassIndex");

    public PluginsClassLoader(PluginAccessor pluginAccessor)
    {
        if (pluginAccessor == null)
        {
            throw new IllegalArgumentException("The plugin accessor should not be null.");
        }
        this.pluginAccessor = pluginAccessor;
    }

    protected URL findResource(final String name)
    {
        final Plugin indexedPlugin = (Plugin) pluginResourceIndex.get(name);
        URL result = null;
        if (isPluginEnabled(indexedPlugin))
        {
            result = indexedPlugin.getClassLoader().getResource(name);
        }
        else
        {
            final Collection plugins = pluginAccessor.getEnabledPlugins();
            for (Iterator i = plugins.iterator(); i.hasNext() && result == null;)
            {
                final Plugin plugin = (Plugin) i.next();
                final URL resource = plugin.getClassLoader().getResource(name);
                if (resource != null)
                {
                    result = resource;
                    pluginResourceIndex.put(name, plugin);
                }
            }
        }
        if (log.isDebugEnabled())
        {
            log.debug("Find resource [ " + name + " ], found [ " + result + " ]");
        }
        return result;
    }

    protected Class findClass(String className) throws ClassNotFoundException
    {
        final Plugin indexedPlugin = (Plugin) pluginClassIndex.get(className);
        Class result = null;
        if (isPluginEnabled(indexedPlugin))
        {
            result = indexedPlugin.getClassLoader().loadClass(className);
        }
        else
        {
            final Collection plugins = pluginAccessor.getEnabledPlugins();
            for (Iterator i = plugins.iterator(); i.hasNext() && result == null;)
            {
                final Plugin plugin = (Plugin) i.next();
                try
                {
                    result = plugin.getClassLoader().loadClass(className);
                    pluginClassIndex.put(className, plugin);
                }
                catch (ClassNotFoundException e)
                {
                    // continue searching the other plugins
                }
            }
        }
        if (log.isDebugEnabled())
        {
            log.debug("Find class [ " + className + " ], found [ " + result + " ]");
        }
        if (result != null)
        {
            return result;
        }
        else
        {
            throw new ClassNotFoundException(className);
        }
    }

    private boolean isPluginEnabled(Plugin plugin)
    {
        return plugin != null && pluginAccessor.isPluginEnabled(plugin.getKey());
    }

    public void notifyUninstallPlugin(Plugin plugin)
    {
        Collection resourceKeys = pluginResourceIndex.getKeys();
        for (Iterator i = resourceKeys.iterator(); i.hasNext();)
        {
            String resourceName = (String) i.next();
            Plugin pluginForResource = (Plugin) pluginResourceIndex.get(resourceName);
            if(plugin.getKey().equals(pluginForResource.getKey())) {
                pluginResourceIndex.remove(resourceName);
            }
        }
        Collection classKeys = pluginClassIndex.getKeys();
        for (Iterator i = classKeys.iterator(); i.hasNext();)
        {
            String className = (String) i.next();
            Plugin pluginForClass = (Plugin) pluginClassIndex.get(className);
            if(plugin.getKey().equals(pluginForClass.getKey())) {
                pluginClassIndex.remove(className);
            }
        }
    }
}