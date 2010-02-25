package com.atlassian.plugin.osgi;

import com.atlassian.plugin.JarPluginArtifact;
import com.atlassian.plugin.AutowireCapablePlugin;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginState;
import com.atlassian.plugin.PluginRestartState;
import com.atlassian.plugin.DefaultModuleDescriptorFactory;
import com.atlassian.plugin.hostcontainer.DefaultHostContainer;
import com.atlassian.plugin.descriptors.RequiresRestart;
import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.module.ModuleClassFactory;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider;
import com.atlassian.plugin.osgi.hostcomponents.ComponentRegistrar;
import com.atlassian.plugin.test.PluginJarBuilder;

import java.io.File;
import java.io.IOException;

import org.osgi.util.tracker.ServiceTracker;

public class TestEnableDisablePlugin extends PluginInContainerTestBase
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

        plugin = pluginManager.getPlugin("enabledisable");

        assertNotNull(((AutowireCapablePlugin)plugin).autowire(plugin.loadClass("my.Foo", this.getClass())));
    }

    public void testEnableDisableEnableWithPublicComponent() throws Exception
    {
        File pluginJar = new PluginJarBuilder("enabledisabletest")
                .addFormattedResource("atlassian-plugin.xml",
                    "<atlassian-plugin name='Test 2' key='enabledisablewithcomponent' pluginsVersion='2'>",
                    "    <plugin-info>",
                    "        <version>1.0</version>",
                    "    </plugin-info>",
                    "    <component key='foo' class='my.Foo' public='true' interface='my.Fooable'/>",
                    "</atlassian-plugin>")
                .addJava("my.Fooable", "package my;" +
                        "public interface Fooable {}")
                .addFormattedJava("my.Foo", "package my;",
                        "public class Foo implements Fooable, org.springframework.beans.factory.DisposableBean {",
                        "  public void destroy() throws Exception { Thread.sleep(500); }",
                        "}")
                .build();
        initPluginManager(null);
        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        Plugin plugin = pluginManager.getPlugin("enabledisablewithcomponent");
        assertEquals(PluginState.ENABLED, plugin.getPluginState());
        assertNotNull(((AutowireCapablePlugin)plugin).autowire(plugin.loadClass("my.Foo", this.getClass())));
        pluginManager.disablePlugin("enabledisablewithcomponent");
        pluginManager.enablePlugin("enabledisablewithcomponent");

        plugin = pluginManager.getPlugin("enabledisablewithcomponent");
        assertEquals(PluginState.ENABLED, plugin.getPluginState());

        assertNotNull(((AutowireCapablePlugin)plugin).autowire(plugin.loadClass("my.Foo", this.getClass())));
    }
    
    public void testDisableEnableOfPluginThatRequiresRestart() throws Exception, IOException
    {
        final DefaultModuleDescriptorFactory factory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
        factory.addModuleDescriptor("requiresRestart", RequiresRestartModuleDescriptor.class);
        new PluginJarBuilder()
                .addFormattedResource("atlassian-plugin.xml",
                    "<atlassian-plugin name='Test 2' key='test.restartrequired' pluginsVersion='2'>",
                    "    <plugin-info>",
                    "        <version>1.0</version>",
                    "    </plugin-info>",
                    "    <requiresRestart key='foo' />",
                    "</atlassian-plugin>")
                .build(pluginsDir);

        initPluginManager(null, factory);

        assertEquals(1, pluginManager.getPlugins().size());
        assertNotNull(pluginManager.getPlugin("test.restartrequired"));
        assertTrue(pluginManager.isPluginEnabled("test.restartrequired"));
        assertEquals(1, pluginManager.getEnabledModuleDescriptorsByClass(RequiresRestartModuleDescriptor.class).size());
        assertEquals(PluginRestartState.NONE, pluginManager.getPluginRestartState("test.restartrequired"));

        pluginManager.disablePlugin("test.restartrequired");
        assertFalse(pluginManager.isPluginEnabled("test.restartrequired"));
        pluginManager.enablePlugin("test.restartrequired");

        assertEquals(1, pluginManager.getPlugins().size());
        assertNotNull(pluginManager.getPlugin("test.restartrequired"));
        assertTrue(pluginManager.isPluginEnabled("test.restartrequired"));
        assertEquals(PluginRestartState.NONE, pluginManager.getPluginRestartState("test.restartrequired"));
        assertEquals(1, pluginManager.getEnabledModuleDescriptorsByClass(RequiresRestartModuleDescriptor.class).size());
    }

    public void testEnableEnablesDependentPlugins() throws Exception
    {
        PluginJarBuilder builderProvider = new PluginJarBuilder("enabledisable-prov")
                .addFormattedResource("atlassian-plugin.xml",
                    "<atlassian-plugin name='Test' key='provider' pluginsVersion='2'>",
                    "    <plugin-info>",
                    "        <version>1.0</version>",
                    "        <bundle-instructions><Export-Package>my</Export-Package></bundle-instructions>",
                    "    </plugin-info>",
                    "</atlassian-plugin>")
                .addJava("my.Foo", "package my;" +
                        "public interface Foo {}");

        PluginJarBuilder builderConsumer = new PluginJarBuilder("enabledisable-con", builderProvider.getClassLoader())
                .addFormattedResource("atlassian-plugin.xml",
                    "<atlassian-plugin name='Test' key='consumer' pluginsVersion='2'>",
                    "    <plugin-info>",
                    "        <version>1.0</version>",
                    "        <bundle-instructions><Import-Package>my</Import-Package></bundle-instructions>",
                    "    </plugin-info>",
                    "</atlassian-plugin>")
                .addJava("my2.Bar", "package my2;" +
                        "public class Bar implements my.Foo {}");

        initPluginManager(null);
        pluginManager.installPlugin(new JarPluginArtifact(builderProvider.build()));
        pluginManager.installPlugin(new JarPluginArtifact(builderConsumer.build()));

        Plugin provider = pluginManager.getPlugin("provider");
        Plugin consumer = pluginManager.getPlugin("consumer");
        assertEquals(PluginState.ENABLED, provider.getPluginState());
        assertEquals(PluginState.ENABLED, consumer.getPluginState());

        pluginManager.disablePlugin("provider");
        pluginManager.disablePlugin("consumer");

        assertEquals(PluginState.DISABLED, provider.getPluginState());
        assertEquals(PluginState.DISABLED, consumer.getPluginState());

        pluginManager.enablePlugin("consumer");
        assertEquals(PluginState.ENABLED, consumer.getPluginState());
        assertEquals(PluginState.ENABLED, provider.getPluginState());
    }

    public void testEnableEnablesDependentPluginsWithBundles() throws Exception
    {
        PluginJarBuilder builderProvider = new PluginJarBuilder("enabledisable-prov")
                .addFormattedResource("META-INF/MANIFEST.MF",
                    "Manifest-Version: 1.0",
                    "Bundle-SymbolicName: my",
                    "Atlassian-Plugin-Key: provider",
                    "Export-Package: my",
                    "")
                .addJava("my.Foo", "package my;" +
                        "public interface Foo {}");



        PluginJarBuilder builderConsumer = new PluginJarBuilder("enabledisable-con", builderProvider.getClassLoader())
                .addFormattedResource("atlassian-plugin.xml",
                    "<atlassian-plugin name='Test' key='consumer' pluginsVersion='2'>",
                    "    <plugin-info>",
                    "        <version>1.0</version>",
                    "        <bundle-instructions><Import-Package>my</Import-Package></bundle-instructions>",
                    "    </plugin-info>",
                    "</atlassian-plugin>")
                .addJava("my2.Bar", "package my2;" +
                        "public class Bar implements my.Foo {}");

        initPluginManager(null);
        pluginManager.installPlugin(new JarPluginArtifact(builderProvider.build()));
        pluginManager.installPlugin(new JarPluginArtifact(builderConsumer.build()));

        Plugin provider = pluginManager.getPlugin("provider");
        Plugin consumer = pluginManager.getPlugin("consumer");
        assertEquals(PluginState.ENABLED, provider.getPluginState());
        assertEquals(PluginState.ENABLED, consumer.getPluginState());

        pluginManager.disablePlugin("provider");
        pluginManager.disablePlugin("consumer");

        assertEquals(PluginState.DISABLED, provider.getPluginState());
        assertEquals(PluginState.DISABLED, consumer.getPluginState());

        pluginManager.enablePlugin("consumer");
        assertEquals(PluginState.ENABLED, consumer.getPluginState());
        assertEquals(PluginState.ENABLED, provider.getPluginState());
    }

    public void testDisableDoesNotKillLongRunningOperation() throws Exception
    {
        File pluginJar = new PluginJarBuilder("longrunning")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test' key='longrunning' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <component key='comp' class='my.Foo' public='true'>",
                        "       <interface>com.atlassian.plugin.osgi.Callable3</interface>",
                        "    </component>",
                        "</atlassian-plugin>")
                .addFormattedJava("my.Foo",
                        "package my;",
                        "import com.atlassian.plugin.osgi.*;",
                        "public class Foo implements Callable3{",
                        "  private Callable2 callable;",
                        "  public Foo(Callable2 callable) {",
                        "    this.callable = callable;",
                        "  }",
                        "  public String call() throws Exception {",
                        "    Thread.sleep(2000);",
                        "    return callable.call();",
                        "  }",
                        "}")
                .build();
        initPluginManager(new HostComponentProvider()
        {
            public void provide(ComponentRegistrar registrar)
            {
                registrar.register(Callable2.class).forInstance(new Callable2()
                {

                    public String call()
                    {
                        return "called";
                    }
                }).withName("foobar");
            }
        });

        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        assertTrue(pluginManager.getPlugin("longrunning").getPluginState() == PluginState.ENABLED);
        final ServiceTracker tracker = osgiContainerManager.getServiceTracker("com.atlassian.plugin.osgi.Callable3");
        final Callable3 service = (Callable3) tracker.getService();
        final StringBuilder sb = new StringBuilder();
        Thread t = new Thread()
        {
            public void run()
            {
                try
                {
                    sb.append(service.call());
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }
        };
        t.start();
        pluginManager.disablePlugin("longrunning");
        t.join();
        assertEquals("called", sb.toString());
    }
    
    @RequiresRestart
    public static class RequiresRestartModuleDescriptor extends AbstractModuleDescriptor
    {
        public RequiresRestartModuleDescriptor()
        {
            super(ModuleClassFactory.NOOP_MODULE_CREATOR);
        }

        @Override
        public Void getModule()
        {
            throw new UnsupportedOperationException("You should never be getting a module from this descriptor " + this.getClass().getName());
        }
    }
}
