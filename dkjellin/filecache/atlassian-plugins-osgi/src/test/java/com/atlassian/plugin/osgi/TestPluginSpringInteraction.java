package com.atlassian.plugin.osgi;

import com.atlassian.plugin.DefaultModuleDescriptorFactory;
import com.atlassian.plugin.hostcontainer.DefaultHostContainer;
import com.atlassian.plugin.osgi.hostcomponents.ComponentRegistrar;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider;
import com.atlassian.plugin.test.PluginJarBuilder;

import java.util.concurrent.Callable;

public class TestPluginSpringInteraction extends PluginInContainerTestBase
{
    public void testDisposable() throws Exception
    {
        StaticBooleanFlag.flag.set(false);
        new PluginJarBuilder("testDisposable")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test' key='test.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <component key='obj' class='my.Foo'/>",
                        "</atlassian-plugin>")
                .addFormattedJava("my.Foo",
                        "package my;",
                        "public class Foo implements org.springframework.beans.factory.DisposableBean{",
                        "  public void destroy() {",
                        "    com.atlassian.plugin.osgi.StaticBooleanFlag.flag.set(true);",
                        "  }",
                        "}")
                .build(pluginsDir);

        initPluginManager();
        assertFalse(StaticBooleanFlag.flag.get());

        // on disable
        pluginManager.disablePlugin("test.plugin");
        assertTrue(StaticBooleanFlag.flag.get());
        pluginManager.enablePlugin("test.plugin");

        // on framework shutdown
        StaticBooleanFlag.flag.set(false);
        osgiContainerManager.stop();
        assertTrue(StaticBooleanFlag.flag.get());
    }
}