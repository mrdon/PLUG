package com.atlassian.plugin.loaders.deployer;

import com.atlassian.plugin.*;
import com.atlassian.plugin.classloader.PluginClassLoader;
import com.atlassian.plugin.loaders.PluginFactory;
import com.atlassian.plugin.loaders.classloading.DeploymentUnit;
import com.atlassian.plugin.parsers.DescriptorParser;
import com.atlassian.plugin.parsers.DescriptorParserFactory;
import com.atlassian.plugin.parsers.XmlDescriptorParserFactory;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;

/**
 * Deploys version 1.0 plugins into the legacy custom classloader structure that gives each plugin its own classloader.
 *
 * @since 2.0.0
 */
public class LegacyDynamicPluginDeployer implements PluginDeployer
{
    private DescriptorParserFactory descriptorParserFactory;
    private String pluginDescriptorFileName;
    private PluginFactory pluginFactory;

    public LegacyDynamicPluginDeployer(String pluginDescriptorFileName, PluginFactory pluginFactory)
    {
        this.descriptorParserFactory = new XmlDescriptorParserFactory();
        this.pluginDescriptorFileName = pluginDescriptorFileName;
        this.pluginFactory = pluginFactory;
    }

    /**
     * Deploys the plugin jar
     * @param deploymentUnit the jar to deploy
     * @param moduleDescriptorFactory The factory for plugin modules
     * @return The instantiated and populated plugin
     * @throws PluginParseException If the descriptor cannot be parsed
     */
    public Plugin deploy(DeploymentUnit deploymentUnit, ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException
    {
        Plugin plugin = null;
        InputStream pluginDescriptor = null;
        PluginClassLoader loader = new PluginClassLoader(deploymentUnit.getPath(), Thread.currentThread().getContextClassLoader());
        try
        {
            if (loader.getLocalResource(pluginDescriptorFileName) == null)
                throw new PluginParseException("No descriptor found in classloader for : " + deploymentUnit);

            pluginDescriptor = loader.getResourceAsStream(pluginDescriptorFileName);
            // The plugin we get back may not be the same (in the case of an UnloadablePlugin), so add what gets returned, rather than the original
            DescriptorParser parser = descriptorParserFactory.getInstance(pluginDescriptor);
            plugin = parser.configurePlugin(moduleDescriptorFactory, pluginFactory.createPlugin(deploymentUnit, loader));
        }
        // Under normal conditions, the deployer would be closed when the plugins are undeployed. However,
        // these are not normal conditions, so we need to make sure that we close them explicitly.
        catch (PluginParseException e)
        {
            loader.close();
            throw e;
        }
        catch (RuntimeException e)
        {
            loader.close();
            throw e;
        }
        catch (Error e)
        {
            loader.close();
            throw e;
        }
        finally
        {
            IOUtils.closeQuietly(pluginDescriptor);
        }
        return plugin;
    }

    /**
     * Determines if this deployer can handle this artifact by looking for the plugin descriptor
     *
     * @param pluginArtifact The artifact to test
     * @return The plugin key, null if it cannot load the plugin
     * @throws com.atlassian.plugin.PluginParseException If there are exceptions parsing the plugin configuration
     */
    public String canDeploy(PluginArtifact pluginArtifact) throws PluginParseException
    {
        String pluginKey = null;
        final InputStream descriptorStream = pluginArtifact.getFile(pluginDescriptorFileName);
        if (descriptorStream != null)
        {
            final DescriptorParser descriptorParser = descriptorParserFactory.getInstance(descriptorStream);

            // Only recognize version 1 plugins
            if (descriptorParser.getPluginsVersion() <= 1)
                pluginKey = descriptorParser.getKey();
        }
        return pluginKey;
    }
}
