package com.atlassian.plugin.loaders;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.Enumeration;
import java.util.Map;
import java.util.Collection;
import java.net.URL;
import java.io.IOException;

import com.atlassian.core.util.ClassLoaderUtils;
import com.atlassian.plugin.PluginManager;

public class ClassPathPluginLoader implements PluginLoader
{
    private static Log log = LogFactory.getLog(ClassPathPluginLoader.class);
    List plugins;

    private void loadClassPathPlugins(Map moduleDescriptors)
    {
        URL url = null;
        final Enumeration pluginDescriptorFiles;

        try
        {
            pluginDescriptorFiles = ClassLoaderUtils.getResources(PluginManager.PLUGIN_DESCRIPTOR_FILENAME, this.getClass());
        }
        catch (IOException e)
        {
            log.error("Could not load classpath plugins: " + e, e);
            return;
        }

        while (pluginDescriptorFiles.hasMoreElements())
        {
                url = (URL) pluginDescriptorFiles.nextElement();
                SinglePluginLoader loader = new SinglePluginLoader(url);
                plugins.addAll(loader.getPlugins(moduleDescriptors));
        }
    }

    public Collection getPlugins(Map moduleDescriptors)
    {
        if (plugins == null)
        {
            loadClassPathPlugins(moduleDescriptors);
        }

        return plugins;
    }
}
