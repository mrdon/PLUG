package com.atlassian.plugin.osgi;

import com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider;
import com.atlassian.plugin.osgi.hostcomponents.ComponentRegistrar;
import com.atlassian.plugin.test.PluginBuilder;
import com.atlassian.plugin.JarPluginArtifact;
import com.atlassian.plugin.DefaultModuleDescriptorFactory;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;

import java.io.File;

public class PluginInstallTest extends PluginInContainerTestBase
{

    public void testUpgradeWithNewComponentImports() throws Exception
    {
        DefaultModuleDescriptorFactory factory = new DefaultModuleDescriptorFactory();
        factory.addModuleDescriptor("dummy", DummyModuleDescriptor.class);
        initPluginManager(new HostComponentProvider(){
            public void provide(ComponentRegistrar registrar)
            {
                registrar.register(SomeInterface.class).forInstance(new SomeInterface(){});
                registrar.register(AnotherInterface.class).forInstance(new AnotherInterface(){});
            }
        }, factory);

        File pluginJar = new PluginBuilder("first")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test' key='test.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <component-import key='comp1' interface='com.atlassian.plugin.osgi.SomeInterface' />",
                        "    <dummy key='dum1'/>",
                        "</atlassian-plugin>")
                .build();
        File pluginJar2 = new PluginBuilder("second")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test 2' key='test.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <component-import key='comp1' interface='com.atlassian.plugin.osgi.SomeInterface' />",
                        "    <component-import key='comp2' interface='com.atlassian.plugin.osgi.AnotherInterface' />",
                        "    <dummy key='dum1'/>",
                        "    <dummy key='dum2'/>",
                        "</atlassian-plugin>")
                .build();

        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        assertEquals(1, pluginManager.getEnabledPlugins().size());
        assertEquals("Test", pluginManager.getPlugin("test.plugin").getName());
        assertEquals(1, pluginManager.getPlugin("test.plugin").getModuleDescriptors().size());
        pluginManager.installPlugin(new JarPluginArtifact(pluginJar2));
        assertEquals(1, pluginManager.getEnabledPlugins().size());
        assertEquals(2, pluginManager.getPlugin("test.plugin").getModuleDescriptors().size());
        assertEquals("Test 2", pluginManager.getPlugin("test.plugin").getName());
    }

    public static class DummyModuleDescriptor extends AbstractModuleDescriptor
    {
        public Object getModule()
        {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }
    }

}
