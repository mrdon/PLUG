package com.atlassian.plugin.osgi;

import com.atlassian.plugin.DefaultModuleDescriptorFactory;
import com.atlassian.plugin.osgi.hostcomponents.ComponentRegistrar;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider;
import com.atlassian.plugin.test.PluginJarBuilder;

import org.apache.commons.io.FileUtils;

/**
 * Tests the plugin framework handling restarts correctly
 */
public class FrameworkRestartTest extends PluginInContainerTestBase
{
    @Override
    public void setUp() throws Exception
    {
        super.setUp();
    }

    public void testMultiplePlugins() throws Exception
    {

        final int numHostComponents = 200;
        final int numPlugins = 50;

        final DefaultModuleDescriptorFactory factory = new DefaultModuleDescriptorFactory();
        factory.addModuleDescriptor("dummy", DummyModuleDescriptor.class);
        final HostComponentProvider prov = new HostComponentProvider()
        {
            public void provide(final ComponentRegistrar registrar)
            {
                for (int x = 0; x < numHostComponents; x++)
                {
                    registrar.register(SomeInterface.class).forInstance(new SomeInterface()
                    {});
                }
            }
        };

        for (int x = 0; x < numPlugins; x++)
        {
            new PluginJarBuilder("restart-test").addFormattedResource("atlassian-plugin.xml",
                "<atlassian-plugin name='Test' key='test.plugin" + x + "'>", "    <plugin-info>", "        <version>1.0</version>",
                "    </plugin-info>", "    <dummy key='dum1'/>", "</atlassian-plugin>").build(pluginsDir);
        }
        int legacyTotal = 0;
        for (int x = 0; x < 10; x++)
        {
            final long start = System.currentTimeMillis();
            initPluginManager(prov, factory);
            final long end = System.currentTimeMillis();
            assertEquals(numPlugins, pluginManager.getEnabledPlugins().size());
            legacyTotal += end - start;
            pluginManager.shutdown();
        }
        FileUtils.cleanDirectory(pluginsDir);

        for (int x = 0; x < numPlugins; x++)
        {
            new PluginJarBuilder("restart-test").addFormattedResource("atlassian-plugin.xml",
                "<atlassian-plugin name='Test' key='test.plugin" + x + "' pluginsVersion='2'>", "    <plugin-info>",
                "        <version>1.0</version>", "    </plugin-info>",
                "    <component-import key='comp1' interface='com.atlassian.plugin.osgi.SomeInterface' />", "    <dummy key='dum1'/>",
                "</atlassian-plugin>").build(pluginsDir);
        }

        // warm up the cache
        initPluginManager(prov, factory, "1.0");
        pluginManager.shutdown();

        int cacheTotal = 0;
        for (int x = 0; x < 10; x++)
        {
            final long start = System.currentTimeMillis();
            initPluginManager(prov, factory, "1.0");
            final long end = System.currentTimeMillis();
            assertEquals(numPlugins, pluginManager.getEnabledPlugins().size());
            cacheTotal += end - start;
            pluginManager.shutdown();
        }

        int noCacheTotal = 0;
        for (int x = 0; x < 10; x++)
        {
            final long start = System.currentTimeMillis();
            initPluginManager(prov, factory);
            final long end = System.currentTimeMillis();
            assertEquals(numPlugins, pluginManager.getEnabledPlugins().size());
            noCacheTotal += end - start;
            pluginManager.shutdown();
        }

        System.out.println("Start speed test - legacy: " + (legacyTotal / 10) + " no caching: " + (noCacheTotal / 10) + " ms  caching: " + (cacheTotal / 10) + " ms");
        //assertTrue(cacheTotal < noCacheTotal);
    }

}
