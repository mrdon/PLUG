package com.atlassian.plugin.osgi.performance;

import com.atlassian.plugin.DefaultModuleDescriptorFactory;
import com.atlassian.plugin.hostcontainer.DefaultHostContainer;
import com.atlassian.plugin.osgi.hostcomponents.ComponentRegistrar;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider;
import com.atlassian.plugin.osgi.PluginInContainerTestBase;
import com.atlassian.plugin.osgi.DummyModuleDescriptor;
import com.atlassian.plugin.osgi.SomeInterface;
import com.atlassian.plugin.test.PluginJarBuilder;

import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.io.File;

/**
 * Tests the plugin framework handling restarts correctly
 */
public class OsgiNoCacheFrameworkRestartTest extends FrameworkRestartTestBase
{

    protected void addPlugin(File dir, int x) throws IOException
    {
        new PluginJarBuilder("restart-test")
                .addFormattedResource("atlassian-plugin.xml",
                    "<atlassian-plugin name='Test' key='test.plugin" + x + "' pluginsVersion='2'>",
                    "    <plugin-info>",
                    "        <version>1.0</version>",
                    "    </plugin-info>",
                    "    <component-import key='comp1' interface='com.atlassian.plugin.osgi.SomeInterface' />",
                    "    <dummy key='dum1'/>",
                    "</atlassian-plugin>")
                .build(pluginsDir);
    }
}