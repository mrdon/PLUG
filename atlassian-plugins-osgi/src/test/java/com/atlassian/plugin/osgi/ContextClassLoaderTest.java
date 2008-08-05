package com.atlassian.plugin.osgi;

import com.atlassian.plugin.JarPluginArtifact;
import com.atlassian.plugin.osgi.hostcomponents.ComponentRegistrar;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider;
import com.atlassian.plugin.osgi.hostcomponents.ContextClassLoaderStrategy;
import com.atlassian.plugin.test.PluginBuilder;
import junit.framework.TestCase;

import java.io.File;

public class ContextClassLoaderTest extends PluginInContainerTestBase {

    public void testCorrectContextClassLoaderForHostComponents() throws Exception
    {
        final HostCompImpl comp = new HostCompImpl();
        File plugin = new PluginBuilder("ccltest")
                .addResource("atlassian-plugin.xml",
                        "<atlassian-plugin key=\"ccltest\" pluginsVersion=\"2\">\n" +
                        "    <plugin-info>\n" +
                        "        <version>1.0</version>\n" +
                        "    </plugin-info>\n" +
                        "    <component key=\"foo\" class=\"my.FooImpl\" />\n" +
                        "</atlassian-plugin>")
                .addJava("my.Foo", "package my;public interface Foo {}")
                .addJava("my.FooImpl", "package my;import com.atlassian.plugin.osgi.HostComp;" +
                        "public class FooImpl implements Foo {public FooImpl(HostComp comp) throws Exception { comp.run(); }}")
                .build();
        HostComponentProvider prov = new HostComponentProvider()
        {
            public void provide(ComponentRegistrar registrar)
            {
                registrar.register(HostComp.class).forInstance(comp).withName("hostComp");
            }
        };

        initPluginManager(prov);
        pluginManager.installPlugin(new JarPluginArtifact(plugin));

        assertNotNull(comp.cl);
        assertNotNull(comp.testClass);
        assertTrue(comp.testClass == TestCase.class);

    }

    public void testCorrectContextClassLoaderForHostComponentsUsePluginStrategy() throws Exception
    {
        final HostCompImpl comp = new HostCompImpl();
        File plugin = new PluginBuilder("ccltest")
                .addResource("atlassian-plugin.xml",
                        "<atlassian-plugin key=\"ccltest\" pluginsVersion=\"2\">\n" +
                        "    <plugin-info>\n" +
                        "        <version>1.0</version>\n" +
                        "    </plugin-info>\n" +
                        "    <component key=\"foo\" class=\"my.FooImpl\" />\n" +
                        "</atlassian-plugin>")
                .addJava("my.Foo", "package my;public interface Foo {}")
                .addJava("my.FooImpl", "package my;import com.atlassian.plugin.osgi.HostComp;" +
                        "public class FooImpl implements Foo {public FooImpl(HostComp comp) throws Exception { comp.run(); }}")
                .build();
        HostComponentProvider prov = new HostComponentProvider()
        {
            public void provide(ComponentRegistrar registrar)
            {
                registrar.register(HostComp.class).forInstance(comp).withName("hostComp").withContextClassLoaderStrategy(ContextClassLoaderStrategy.USE_PLUGIN);
            }
        };

        initPluginManager(prov);
        pluginManager.installPlugin(new JarPluginArtifact(plugin));

        assertNotNull(comp.cl);
        assertNull(comp.testClass);
    }

    public void testCorrectContextClassLoaderForHostComponentsUseHostStrategy() throws Exception
    {
        final HostCompImpl comp = new HostCompImpl();
        File plugin = new PluginBuilder("ccltest")
                .addResource("atlassian-plugin.xml",
                        "<atlassian-plugin key=\"ccltest\" pluginsVersion=\"2\">\n" +
                        "    <plugin-info>\n" +
                        "        <version>1.0</version>\n" +
                        "    </plugin-info>\n" +
                        "    <component key=\"foo\" class=\"my.FooImpl\" />\n" +
                        "</atlassian-plugin>")
                .addJava("my.Foo", "package my;public interface Foo {}")
                .addJava("my.FooImpl", "package my;import com.atlassian.plugin.osgi.HostComp;" +
                        "public class FooImpl implements Foo {public FooImpl(HostComp comp) throws Exception { comp.run(); }}")
                .build();
        HostComponentProvider prov = new HostComponentProvider()
        {
            public void provide(ComponentRegistrar registrar)
            {
                registrar.register(HostComp.class).forInstance(comp).withName("hostComp").withContextClassLoaderStrategy(ContextClassLoaderStrategy.USE_HOST);
            }
        };

        initPluginManager(prov);
        pluginManager.installPlugin(new JarPluginArtifact(plugin));

        assertNotNull(comp.cl);
        assertNotNull(comp.testClass);
        assertTrue(comp.testClass == TestCase.class);
    }

    public static class HostCompImpl implements HostComp
    {
        public ClassLoader cl;
        public Class testClass;
        public void run()
        {
            cl = Thread.currentThread().getContextClassLoader();
            try
            {
                testClass = cl.loadClass("junit.framework.TestCase");
            } catch (ClassNotFoundException ex) {}
        }
    }
}
