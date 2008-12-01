package com.atlassian.plugin.loaders;

import com.atlassian.plugin.ModuleDescriptorFactory;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.PluginException;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.util.ClassLoaderUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

/**
 * Loads plugins from the classpath
 */
public class ClassPathPluginLoader implements PluginLoader
{
    private static Log log = LogFactory.getLog(ClassPathPluginLoader.class);

    private final String fileNameToLoad;
    private List<Plugin> plugins;

    public ClassPathPluginLoader()
    {
        this(PluginAccessor.Descriptor.FILENAME);
    }

    public ClassPathPluginLoader(final String fileNameToLoad)
    {
        this.fileNameToLoad = fileNameToLoad;
    }

    private void loadClassPathPlugins(final ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException
    {
        URL url = null;
        final Enumeration<URL> pluginDescriptorFiles;
        plugins = new ArrayList<Plugin>();

        try
        {
            pluginDescriptorFiles = ClassLoaderUtils.getResources(fileNameToLoad, this.getClass());
        }
        catch (final IOException e)
        {
            log.error("Could not load classpath plugins: " + e, e);
            return;
        }

        while (pluginDescriptorFiles.hasMoreElements())
        {
            url = pluginDescriptorFiles.nextElement();
            try
            {
                final SinglePluginLoader loader = new SinglePluginLoader(url.openConnection().getInputStream());
                plugins.addAll(loader.loadAllPlugins(moduleDescriptorFactory));
            }
            catch (final IOException e)
            {
                log.error("IOException parsing inputstream for : " + url, e);
            }
            catch (final PluginParseException e)
            {
                log.error("Unable to load plugin at url: " + url + ", " + e.getMessage(), e);
            }
        }
    }

    public Collection<Plugin> loadAllPlugins(final ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException
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

    public Collection<Plugin> addFoundPlugins(final ModuleDescriptorFactory moduleDescriptorFactory)
    {
        throw new UnsupportedOperationException("This PluginLoader does not support addition.");
    }

    public void removePlugin(final Plugin plugin) throws PluginException
    {
        throw new PluginException("This PluginLoader does not support removal.");
    }
}
