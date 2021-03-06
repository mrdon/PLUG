package com.atlassian.plugin.osgi;

import com.atlassian.plugin.test.PluginJarBuilder;
import com.atlassian.plugin.JarPluginArtifact;

import java.io.File;

public class TestShutdown extends PluginInContainerTestBase
{
    public void testShutdown() throws Exception
    {
        File pluginJar = new PluginJarBuilder("shutdowntest")
                .addPluginInformation("shutdown", "foo", "1.0")
                .build();
        initPluginManager(null);
        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        pluginManager.shutdown();
    }
}

