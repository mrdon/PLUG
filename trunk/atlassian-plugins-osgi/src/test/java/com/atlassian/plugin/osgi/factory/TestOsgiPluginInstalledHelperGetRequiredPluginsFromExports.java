package com.atlassian.plugin.osgi.factory;

import java.util.Collections;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.osgi.PluginInContainerTestBase;
import com.atlassian.plugin.test.PluginJarBuilder;

public class TestOsgiPluginInstalledHelperGetRequiredPluginsFromExports extends PluginInContainerTestBase
{
    public void testEnablingDisabledDependentPluginRecursivelyEnablesDependency() throws Exception
    {
        new PluginJarBuilder("osgi")
                .addFormattedResource("META-INF/MANIFEST.MF",
                    "Manifest-Version: 1.0",
                    "Bundle-SymbolicName: myA",
                    "Bundle-Version: 1.0",
                    "Export-Package: testpackage",
                    "")
                .addFormattedResource("atlassian-plugin.xml",
                    "<atlassian-plugin name='Test' key='myA-1.0' pluginsVersion='2' state='disabled'>",
                    "    <plugin-info>",
                    "        <version>1.0</version>",
                    "    </plugin-info>",
                    "</atlassian-plugin>")
                .build(pluginsDir);
        
        new PluginJarBuilder("osgi")
                .addFormattedResource("atlassian-plugin.xml",
                    "<atlassian-plugin name='Test' key='consumer' pluginsVersion='2' state='disabled'>",
                    "    <plugin-info>",
                    "        <version>1.0</version>",
                    "        <bundle-instructions><Import-Package>testpackage</Import-Package></bundle-instructions>",
                    "    </plugin-info>",
                    "</atlassian-plugin>")
                .build(pluginsDir);
        
        initPluginManager();

        Plugin plugin = pluginManager.getPlugin("consumer");
        assertEquals(Collections.singleton("myA-1.0"), plugin.getRequiredPlugins());
    }
}
