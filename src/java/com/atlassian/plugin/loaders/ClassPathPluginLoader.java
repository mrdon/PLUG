package com.atlassian.plugin.loaders;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;
import java.net.URL;
import java.io.IOException;

import com.atlassian.core.util.ClassLoaderUtils;
import com.atlassian.plugin.PluginManager;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.ModuleDescriptorFactory;

public class ClassPathPluginLoader implements PluginLoader
{
    private static Log log = LogFactory.getLog(ClassPathPluginLoader.class);
    List plugins;
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
        plugins = new ArrayList();
        
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
                plugins.addAll(loader.getPlugins(moduleDescriptorFactory));
            }
            catch (IOException e)
            {
                log.error("IOException parsing inputstream for : " + url, e);
            }
        }
    }

    public Collection getPlugins(ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException
    {
        if (plugins == null)
        {
            loadClassPathPlugins(moduleDescriptorFactory);
        }

        return plugins;
    }
}
