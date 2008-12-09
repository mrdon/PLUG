package com.atlassian.plugin.main;

import static com.atlassian.plugin.main.PluginsConfigurationBuilder.pluginsConfiguration;

import com.atlassian.plugin.test.PluginJarBuilder;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

public class TestAtlassianPlugins extends TestCase
{
    File pluginDir;
    AtlassianPlugins plugins;

    @Override
    public void setUp()
    {
        final File targetDir = new File("target");
        pluginDir = new File(targetDir, "plugins");
        pluginDir.mkdir();
    }

    @Override
    public void tearDown() throws IOException
    {
        FileUtils.cleanDirectory(pluginDir);
        if (plugins != null)
        {
            plugins.stop();
        }
    }

    public void testStart() throws Exception
    {
        //tests
        new PluginJarBuilder().addPluginInformation("mykey", "mykey", "1.0").build(pluginDir);
        final PluginsConfiguration config = pluginsConfiguration().pluginDirectory(pluginDir).packageScannerConfiguration(
            new PackageScannerConfigurationBuilder().packagesToInclude("org.apache.*", "com.atlassian.*", "org.dom4j*").build()).build();
        plugins = new AtlassianPlugins(config);
        plugins.start();
        assertEquals(1, plugins.getPluginAccessor().getPlugins().size());
    }
}
