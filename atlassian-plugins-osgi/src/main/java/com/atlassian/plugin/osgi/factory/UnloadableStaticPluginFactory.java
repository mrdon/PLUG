package com.atlassian.plugin.osgi.factory;

import com.atlassian.plugin.JarPluginArtifact;
import com.atlassian.plugin.ModuleDescriptorFactory;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginArtifact;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.factories.PluginFactory;
import com.atlassian.plugin.impl.UnloadablePlugin;
import com.atlassian.plugin.loaders.classloading.DeploymentUnit;
import com.atlassian.plugin.parsers.DescriptorParser;
import com.atlassian.plugin.parsers.XmlDescriptorParserFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;

import java.io.InputStream;

/**
 * Creates unloadable plugins from static plugins.  Used to handle when a static plugin (version 1) is deployed
 * to a directory that only accepts OSGi plugins.  This should be placed last in the chain of plugin factories and
 * only if {@link com.atlassian.plugin.factories.LegacyDynamicPluginFactory} is not used.
 *
 * @since 2.2.3
 */
public class UnloadableStaticPluginFactory implements PluginFactory
{

    private final String pluginDescriptorFileName;
    private final XmlDescriptorParserFactory descriptorParserFactory;

    public UnloadableStaticPluginFactory(String pluginDescriptorFileName)
    {
        this.pluginDescriptorFileName = pluginDescriptorFileName;
        this.descriptorParserFactory = new XmlDescriptorParserFactory();
    }

    public String canCreate(PluginArtifact pluginArtifact) throws PluginParseException
    {
        Validate.notNull(pluginArtifact, "The plugin artifact is required");

        InputStream descriptorStream = null;
        try
        {
            descriptorStream = pluginArtifact.getResourceAsStream(pluginDescriptorFileName);

            if (descriptorStream != null)
            {
                final DescriptorParser descriptorParser = descriptorParserFactory.getInstance(descriptorStream);
                if (descriptorParser.getPluginsVersion() == 1)
                {
                    // This is a version 1 plugin inside the version 2 plugin directory - we want to create an
                    // UnloadablePlugin with appropriate error message.
                    return descriptorParser.getKey();
                }
            }
        }
        finally
        {
            IOUtils.closeQuietly(descriptorStream);
        }
        return null;
    }

    /**
     * @deprecated Since 2.2.0, use {@link #create(PluginArtifact,ModuleDescriptorFactory)} instead
     */
    public Plugin create(DeploymentUnit deploymentUnit, ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException
    {
        Validate.notNull(deploymentUnit, "The deployment unit is required");
        return create(new JarPluginArtifact(deploymentUnit.getPath()), moduleDescriptorFactory);
    }

    /**
     * Creates an unloadable plugin
     *
     * @param pluginArtifact          the plugin artifact to deploy
     * @param moduleDescriptorFactory The factory for plugin modules
     * @return The instantiated and populated plugin
     * @throws PluginParseException If the descriptor cannot be parsed
     */
    public Plugin create(PluginArtifact pluginArtifact, ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException
    {
        Validate.notNull(pluginArtifact, "The plugin deployment unit is required");
        Validate.notNull(moduleDescriptorFactory, "The module descriptor factory is required");

        UnloadablePlugin plugin = null;
        InputStream pluginDescriptor = null;
        try
        {
            pluginDescriptor = pluginArtifact.getResourceAsStream(pluginDescriptorFileName);
            if (pluginDescriptor == null)
            {
                throw new PluginParseException("No descriptor found in classloader for : " + pluginArtifact);
            }

            DescriptorParser parser = descriptorParserFactory.getInstance(pluginDescriptor);

            plugin = new UnloadablePlugin();
            // This should be a valid plugin, it just got put in the wrong directory.
            // We'll try to do a full configure because it looks more user-friendly.
            try
            {
                parser.configurePlugin(moduleDescriptorFactory, plugin);
            }
            catch (PluginParseException ex)
            {
                // Error on full configure - we'll just set the key as this is an UnloadablePlugin anyway.
                plugin.setKey(parser.getKey());   
            }
            plugin.setErrorText("Unable to load the static '" + pluginArtifact + "' plugin from the plugins directory.  Please " +
                                "copy this file into WEB-INF/lib and restart.");
        }
        finally
        {
            IOUtils.closeQuietly(pluginDescriptor);
        }
        return plugin;
    }
}
