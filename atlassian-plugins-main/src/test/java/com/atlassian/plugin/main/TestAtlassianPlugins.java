package com.atlassian.plugin.main;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import com.atlassian.plugin.test.PluginJarBuilder;
import org.apache.commons.io.FileUtils;

public class TestAtlassianPlugins extends TestCase
{
    File pluginDir;
    AtlassianPlugins plugins;

    @Override
    public void setUp()
    {
        File targetDir = new File("target");
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


        new PluginJarBuilder()
                .addPluginInformation("mykey", "mykey", "1.0")
                .build(pluginDir);
        PluginsConfiguration config = new PluginsConfigurationBuilder()
                .setPluginDirectory(pluginDir)
                .setPackagesToInclude("org.apache.*", "com.atlassian.*", "org.dom4j*")
                .build();
        plugins = new AtlassianPlugins(config);
        plugins.start();
        assertEquals(1, plugins.getPluginAccessor().getPlugins().size());


    }
}
