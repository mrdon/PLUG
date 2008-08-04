package com.atlassian.plugin.loaders;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;
import java.net.URL;
import java.io.IOException;

import com.atlassian.plugin.*;
import com.atlassian.plugin.util.ClassLoaderUtils;

/**
 * Loads plugins from the classpath
 */
public class ClassPathPluginLoader implements PluginLoader
{
    private static Log log = LogFactory.getLog(ClassPathPluginLoader.class);
    List<Plugin> plugins;
    String fileNameToLoad;

    public ClassPathPluginLoader()
    {
        this(PluginManager.PLUGIN_DESCRIPTOR_FILENAME);
    }

    public ClassPathPluginLoader(String fileNameToLoad)
    {
        this.fileNameToLoad = fileNameToLoad;
    }

    private void loadClassPathPlugins(ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException {
        URL url = null;
        final Enumeration pluginDescriptorFiles;
        plugins = new ArrayList<Plugin>();
        
        try
        {
            pluginDescriptorFiles = ClassLoaderUtils.getResources(fileNameToLoad, this.getClass());
        }
        catch (IOException e)
        {
            log.error("Could not load classpath plugins: " + e, e);
            return;
        }

        while (pluginDescriptorFiles.hasMoreElements())
        {
            url = (URL) pluginDescriptorFiles.nextElement();
            try
            {
                SinglePluginLoader loader = new SinglePluginLoader(url.openConnection().getInputStream());
                plugins.addAll(loader.loadAllPlugins(moduleDescriptorFactory));
            }
            catch (IOException e)
            {
                log.error("IOException parsing inputstream for : " + url, e);
            }
            catch (PluginParseException e)
            {
                log.error("Unable to load plugin at url: " + url + ", " + e.getMessage(), e);
            }
        }
    }

    public Collection<Plugin> loadAllPlugins(ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException
    {
        if (plugins == null)
        {
            loadClassPathPlugins(moduleDescriptorFactory);
        }

        return plugins;
    }

    public boolean supportsRemoval()
    {
        return false;
    }

    public boolean supportsAddition()
    {
        return false;
    }

    public Collection<Plugin> addFoundPlugins(ModuleDescriptorFactory moduleDescriptorFactory)
    {
        throw new UnsupportedOperationException("This PluginLoader does not support addition.");
    }

    public void removePlugin(Plugin plugin) throws PluginException
    {
        throw new PluginException("This PluginLoader does not support removal.");
    }
}
