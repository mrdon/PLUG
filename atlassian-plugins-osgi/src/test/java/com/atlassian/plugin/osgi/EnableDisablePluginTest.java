package com.atlassian.plugin.osgi;

import com.atlassian.plugin.JarPluginArtifact;
import com.atlassian.plugin.AutowireCapablePlugin;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginState;
import com.atlassian.plugin.util.WaitUntil;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider;
import com.atlassian.plugin.osgi.hostcomponents.ComponentRegistrar;
import com.atlassian.plugin.test.PluginJarBuilder;

import java.io.File;

import org.osgi.util.tracker.ServiceTracker;

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

        plugin = pluginManager.getPlugin("enabledisable");
        assertNotNull(((AutowireCapablePlugin)plugin).autowire(plugin.loadClass("my.Foo", this.getClass())));
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
}
