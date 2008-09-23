package com.atlassian.plugin.osgi;

import com.atlassian.plugin.test.PluginBuilder;
import com.atlassian.plugin.JarPluginArtifact;

import java.io.File;

public class ShutdownTest extends PluginInContainerTestBase
{
    public void testShutdown() throws Exception
    {
        File pluginJar = new PluginBuilder("shutdowntest")
                .addPluginInformation("shutdown", "foo", "1.0")
                .build();
        initPluginManager(null);
        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        Thread.sleep(3000);
        pluginManager.shutdown();
    }
}

