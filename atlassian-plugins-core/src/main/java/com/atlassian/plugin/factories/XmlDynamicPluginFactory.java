package com.atlassian.plugin.factories;

import com.atlassian.plugin.*;
import com.atlassian.plugin.impl.XmlDynamicPlugin;
import com.atlassian.plugin.loaders.classloading.DeploymentUnit;
import com.atlassian.plugin.parsers.DescriptorParser;
import com.atlassian.plugin.parsers.DescriptorParserFactory;
import com.atlassian.plugin.parsers.XmlDescriptorParserFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.dom4j.DocumentException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Deploys plugins that consist of an XML descriptor file.
 *
 * @since 2.1.0
 */
public class XmlDynamicPluginFactory implements PluginFactory
{
    private DescriptorParserFactory descriptorParserFactory;

    public XmlDynamicPluginFactory()
    {
        this.descriptorParserFactory = new XmlDescriptorParserFactory();
    }

    /**
     * @deprecated Since 2.2.0, use {@link #create(PluginArtifact,ModuleDescriptorFactory)} instead
     */
    public Plugin create(DeploymentUnit deploymentUnit, ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException
    {
        return create(new XmlPluginArtifact(deploymentUnit.getPath()), moduleDescriptorFactory);
    }

    /**
     * Deploys the plugin artifact
     * @param pluginArtifact the plugin artifact to deploy
     * @param moduleDescriptorFactory The factory for plugin modules
     * @return The instantiated and populated plugin
     * @throws PluginParseException If the descriptor cannot be parsed
     * @since 2.2.0
     */
    public Plugin create(PluginArtifact pluginArtifact, ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException
    {
        Validate.notNull(pluginArtifact, "The deployment unit must not be null");
        Validate.notNull(moduleDescriptorFactory, "The module descriptor factory must not be null");

        Plugin plugin = null;
        InputStream pluginDescriptor = null;
        try
        {
            pluginDescriptor = new FileInputStream(pluginArtifact.toFile());
            // The plugin we get back may not be the same (in the case of an UnloadablePlugin), so add what gets returned, rather than the original
            DescriptorParser parser = descriptorParserFactory.getInstance(pluginDescriptor);
            plugin = parser.configurePlugin(moduleDescriptorFactory, new XmlDynamicPlugin());
        }
        catch (RuntimeException e)
        {
            throw new PluginParseException(e);
        }
        catch (IOException e)
        {
            throw new PluginParseException();
        } finally
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
    public String canCreate(PluginArtifact pluginArtifact) throws PluginParseException
    {
        Validate.notNull(pluginArtifact, "The plugin artifact must not be null");
        String pluginKey = null;
        final InputStream descriptorStream = pluginArtifact.getInputStream();
        if (descriptorStream != null)
        {
            try
            {
                final DescriptorParser descriptorParser = descriptorParserFactory.getInstance(descriptorStream);
                pluginKey = descriptorParser.getKey();
            } catch (PluginParseException ex)
            {
                if (!(ex.getCause() instanceof DocumentException))
                    throw ex;
            } finally
            {
                IOUtils.closeQuietly(descriptorStream);
            }
        }
        return pluginKey;
    }
}