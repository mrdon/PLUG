package com.atlassian.plugin.classloader;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

/**
 *
 */
public class PluginsClassLoader extends AbstractClassLoader
{
    private static final Log log = LogFactory.getLog(PluginsClassLoader.class);
    private final PluginAccessor pluginAccessor;

    private final Map<String,Plugin> pluginResourceIndex = new HashMap<String,Plugin>();
    private final Map<String,Plugin> pluginClassIndex = new HashMap<String,Plugin>();

    private final Set<String> missedPluginResource = new HashSet<String>();
    private final Set<String> missedPluginClass = new HashSet<String>();

    public PluginsClassLoader(PluginAccessor pluginAccessor)
    {
        this(null,pluginAccessor);
    }

    public PluginsClassLoader(ClassLoader parent, PluginAccessor pluginAccessor)
    {
        super(parent);
        if (pluginAccessor == null)
        {
            throw new IllegalArgumentException("The plugin accessor should not be null.");
        }
        this.pluginAccessor = pluginAccessor;
    }

    protected URL findResource(final String name)
    {
        final Plugin indexedPlugin;
        synchronized (this)
        {
             indexedPlugin = pluginResourceIndex.get(name);
        }
        final URL result;
        if (isPluginEnabled(indexedPlugin))
        {
            result = indexedPlugin.getClassLoader().getResource(name);
        }
        else
        {
            result = getResourceFromPlugins(name);
        }
        if (log.isDebugEnabled())
        {
            log.debug("Find resource [ " + name + " ], found [ " + result + " ]");
        }
        return result;
    }

    protected Class findClass(String className) throws ClassNotFoundException
    {
        final Plugin indexedPlugin;
        synchronized (this)
        {
            indexedPlugin = pluginClassIndex.get(className);
        }

        final Class result;
        if (isPluginEnabled(indexedPlugin))
        {
            result = indexedPlugin.getClassLoader().loadClass(className);
        }
        else
        {
            result = loadClassFromPlugins(className);
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

    private Class loadClassFromPlugins(String className)
    {
        final boolean isMissedClassName;
        synchronized (this)
        {
            isMissedClassName = missedPluginClass.contains(className);
        }
        if (isMissedClassName)
        {
            return null;
        }
        final Collection<Plugin> plugins = pluginAccessor.getEnabledPlugins();
        for (Plugin plugin : plugins)
        {
            try
            {
                Class result = plugin.getClassLoader().loadClass(className);
                //loadClass should never return null
                synchronized (this)
                {
                    pluginClassIndex.put(className, plugin);
                }
                return result;
            }
            catch (ClassNotFoundException e)
            {
                // continue searching the other plugins
            }
        }
        synchronized (this)
        {
            missedPluginClass.add(className);
        }
        return null;
    }

    private URL getResourceFromPlugins(String name)
    {
        final boolean isMissedResource;
        synchronized (this)
        {
            isMissedResource = missedPluginResource.contains(name);
        }
        if (isMissedResource)
        {
            return null;
        }
        final Collection<Plugin> plugins = pluginAccessor.getEnabledPlugins();
        for (Plugin plugin : plugins)
        {
            final URL resource = plugin.getClassLoader().getResource(name);
            if (resource != null)
            {
                synchronized (this)
                {
                    pluginResourceIndex.put(name, plugin);
                }
                return resource;
            }
        }
        synchronized (this)
        {
            missedPluginResource.add(name);
        }
        return null;
    }

    private boolean isPluginEnabled(Plugin plugin)
    {
        return plugin != null && pluginAccessor.isPluginEnabled(plugin.getKey());
    }

    public synchronized void notifyUninstallPlugin(Plugin plugin)
    {
        flushMissesCaches();
        for (Iterator it = pluginResourceIndex.entrySet().iterator(); it.hasNext();)
        {
            final Map.Entry resourceEntry = (Map.Entry) it.next();
            final Plugin pluginForResource = (Plugin) resourceEntry.getValue();
            if (plugin.getKey().equals(pluginForResource.getKey()))
            {
                it.remove();
            }
        }
        for (Iterator it = pluginClassIndex.entrySet().iterator(); it.hasNext();)
        {
            final Map.Entry pluginClassEntry = (Map.Entry) it.next();
            final Plugin pluginForClass = (Plugin) pluginClassEntry.getValue();
            if (plugin.getKey().equals(pluginForClass.getKey()))
            {
                it.remove();
            }
        }
    }

    public synchronized void notifyPluginOrModuleEnabled()
    {
        flushMissesCaches();
    }

    private void flushMissesCaches()
    {
        missedPluginClass.clear();
        missedPluginResource.clear();
    }
}