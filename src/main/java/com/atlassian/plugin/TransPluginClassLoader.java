package com.atlassian.plugin;

import com.atlassian.plugin.impl.DynamicPlugin;
import com.atlassian.plugin.util.MultiDelegationClassLoader;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

/**
 * This class loader will search the class loaders of all enabled dynamic plugins if it cannot find a class in the
 * default class loader.
 */
public class TransPluginClassLoader extends MultiDelegationClassLoader
{
    private PluginAccessor pluginAccessor;

    protected List/*<ClassLoader>*/ getClassLoaders()
    {
        Collection allPlugins = pluginAccessor.getEnabledPlugins();

        List classLoaders = new ArrayList(allPlugins.size());

        for (Iterator it = allPlugins.iterator(); it.hasNext();)
        {
            Plugin p = (Plugin) it.next();
            if (p instanceof DynamicPlugin)
            {
                DynamicPlugin dp = (DynamicPlugin) p;
                classLoaders.add(dp.getClassLoader());
            }
        }

        return classLoaders;
    }

    public void setPluginManager(PluginAccessor pluginAccessor)
    {
        this.pluginAccessor = pluginAccessor;
    }
}
