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

/**
 *
 */
public class PluginsClassLoader extends AbstractClassLoader
{
    private static final Log log = LogFactory.getLog(PluginsClassLoader.class);
    private final PluginAccessor pluginAccessor;

    private final Map/*<String,Plugin>*/ pluginResourceIndex = new HashMap();
    private final Map/*<String,Plugin>*/ pluginClassIndex = new HashMap();

    private static final Object MARKER_OBJECT = new Object();

    private final Map/*<String,Plugin>*/ missedPluginResourceIndex = new HashMap();
    private final Map/*<String,Plugin>*/ missedPluginClassIndex = new HashMap();

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
        final Plugin indexedPlugin = (Plugin) pluginResourceIndex.get(name);
        final URL result;
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
        final Class result;
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

    public void notifyPluginOrModuleEnabled()
    {
        flushMissesCaches();
    }

    private void flushMissesCaches()
    {
        missedPluginClassIndex.clear();
        missedPluginResourceIndex.clear();
    }
}