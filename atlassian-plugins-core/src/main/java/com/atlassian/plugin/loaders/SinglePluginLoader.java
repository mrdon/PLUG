package com.atlassian.plugin.loaders;

import static com.atlassian.plugin.util.Assertions.notNull;

import com.atlassian.plugin.ModuleDescriptorFactory;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginException;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.impl.StaticPlugin;
import com.atlassian.plugin.parsers.DescriptorParser;
import com.atlassian.plugin.parsers.DescriptorParserFactory;
import com.atlassian.plugin.parsers.XmlDescriptorParserFactory;
import com.atlassian.plugin.util.ClassLoaderUtils;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

/**
 * Loads a single plugin from the descriptor provided, which can either be an InputStream
 * or a resource on the classpath. The classes used by the plugin must already be available
 * on the classpath because this plugin loader does <b>not</b> load any classes.
 * <p/>
 * Because the code which is run by these plugins must already be in the classpath (and
 * is therefore more trusted than an uploaded plugin), if the plugin is marked as a system
 * plugin in the descriptor file, it will actually be marked as a system plugin at runtime.
 *
 * @see PluginLoader
 * @see ClassPathPluginLoader
 * @see DescriptorParser#isSystemPlugin()
 */
public class SinglePluginLoader implements PluginLoader
{
    protected Collection<Plugin> plugins;
    protected final String resource;
    protected final InputStream is;
    private final DescriptorParserFactory descriptorParserFactory = new XmlDescriptorParserFactory();

    public SinglePluginLoader(final String resource)
    {
        this.resource = notNull("resource", resource);
        is = null;
    }

    public SinglePluginLoader(final InputStream is)
    {
        this.is = notNull("inputStream", is);
        resource = null;
    }

    public Collection<Plugin> loadAllPlugins(final ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException
    {
        if (plugins == null)
        {
            plugins = Collections.singleton(loadPlugin(moduleDescriptorFactory));
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

    protected Plugin loadPlugin(final ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException
    {
        final InputStream source = getSource();
        if (source == null)
        {
            throw new PluginParseException("Invalid resource or inputstream specified to load plugins from.");
        }

        Plugin plugin;
        try
        {
            final DescriptorParser parser = descriptorParserFactory.getInstance(source);
            plugin = parser.configurePlugin(moduleDescriptorFactory, getNewPlugin());
            if (parser.isSystemPlugin())
            {
                plugin.setSystemPlugin(true);
            }
        }
        catch (final PluginParseException e)
        {
            throw new PluginParseException("Unable to load plugin resource: " + resource + " - " + e.getMessage(), e);
        }

        return plugin;
    }

    protected StaticPlugin getNewPlugin()
    {
        return new StaticPlugin();
    }

    protected InputStream getSource()
    {
        if (resource == null)
        {
            return is;
        }

        return ClassLoaderUtils.getResourceAsStream(resource, this.getClass());
    }
}