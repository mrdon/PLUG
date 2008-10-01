package com.atlassian.plugin.osgi;

import com.atlassian.plugin.AutowireCapablePlugin;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.artifact.JarPluginArtifact;
import com.atlassian.plugin.test.PluginJarBuilder;

import java.io.File;

public class EnableDisablePluginTest extends PluginInContainerTestBase
{
    public void testEnableDisableEnable() throws Exception
    {
        File pluginJar = new PluginJarBuilder("enabledisabletest")
                .addPluginInformation("enabledisable", "foo", "1.0")
                .addJava("my.Foo", "package my;" +
                        "public class Foo {}")
                .build();
        initPluginManager(null);
        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        Plugin plugin = pluginManager.getPlugin("enabledisable");
        assertNotNull(((AutowireCapablePlugin)plugin).autowire(plugin.loadClass("my.Foo", this.getClass())));
        pluginManager.disablePlugin("enabledisable");
        pluginManager.enablePlugin("enabledisable");
        Thread.sleep(3000);
        plugin = pluginManager.getPlugin("enabledisable");
        assertNotNull(((AutowireCapablePlugin)plugin).autowire(plugin.loadClass("my.Foo", this.getClass())));
    }
}
