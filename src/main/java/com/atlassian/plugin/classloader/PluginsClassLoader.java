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

    private static final Object MARKER_OBJECT = new Object();

    private final Cache/*<String,Plugin>*/ missedPluginResourceIndex = new MemoryCache(this.getClass().getName() + "#MissedResourceIndex");
    private final Cache/*<String,Plugin>*/ missedPluginClassIndex = new MemoryCache(this.getClass().getName() + "#MissedClassIndex");

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
            result = getUncachedResource(name);
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
            result = getUncachedClass(className);
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

    private Class getUncachedClass(String className)
    {
        if (missedPluginClassIndex.get(className) == MARKER_OBJECT)
        {
            return null;
        }
        final Collection plugins = pluginAccessor.getEnabledPlugins();
        for (Iterator i = plugins.iterator(); i.hasNext();)
        {
            final Plugin plugin = (Plugin) i.next();
            try
            {
                Class result = plugin.getClassLoader().loadClass(className);
                //loadClass should never return null
                pluginClassIndex.put(className, plugin);
                return result;
            }
            catch (ClassNotFoundException e)
            {
                // continue searching the other plugins
            }
        }
        missedPluginClassIndex.put(className, MARKER_OBJECT);
        return null;
    }

    private URL getUncachedResource(String name)
    {
        if (missedPluginResourceIndex.get(name) == MARKER_OBJECT)
        {
            return null;
        }
        final Collection plugins = pluginAccessor.getEnabledPlugins();
        for (Iterator i = plugins.iterator(); i.hasNext();)
        {
            final Plugin plugin = (Plugin) i.next();
            final URL resource = plugin.getClassLoader().getResource(name);
            if (resource != null)
            {
                pluginResourceIndex.put(name, plugin);
                return resource;
            }
        }
        missedPluginResourceIndex.put(name, MARKER_OBJECT);
        return null;
    }

    private boolean isPluginEnabled(Plugin plugin)
    {
        return plugin != null && pluginAccessor.isPluginEnabled(plugin.getKey());
    }

    public void notifyUninstallPlugin(Plugin plugin)
    {
        flushMissesCaches();
        Collection resourceKeys = pluginResourceIndex.getKeys();
        for (Iterator i = resourceKeys.iterator(); i.hasNext();)
        {
            String resourceName = (String) i.next();
            Plugin pluginForResource = (Plugin) pluginResourceIndex.get(resourceName);
            if (plugin.getKey().equals(pluginForResource.getKey()))
            {
                pluginResourceIndex.remove(resourceName);
            }
        }
        Collection classKeys = pluginClassIndex.getKeys();
        for (Iterator i = classKeys.iterator(); i.hasNext();)
        {
            String className = (String) i.next();
            Plugin pluginForClass = (Plugin) pluginClassIndex.get(className);
            if (plugin.getKey().equals(pluginForClass.getKey()))
            {
                pluginClassIndex.remove(className);
            }
        }
    }

    public void notifyPluginOrModuleEnabled()
    {
        flushMissesCaches();
    }

    private void flushMissesCaches()
    {
        missedPluginClassIndex.removeAll();
        missedPluginResourceIndex.removeAll();
    }
}