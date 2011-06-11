package com.atlassian.plugin.osgi;

import com.atlassian.plugin.JarPluginArtifact;
import com.atlassian.plugin.loaders.classloading.DeploymentUnit;
import com.atlassian.plugin.osgi.hostcomponents.ComponentRegistrar;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider;
import com.atlassian.plugin.osgi.hostcomponents.ContextClassLoaderStrategy;
import com.atlassian.plugin.test.PluginJarBuilder;
import junit.framework.TestCase;

import java.io.File;

public class TestContextClassLoader extends PluginInContainerTestBase {

    public void testCorrectContextClassLoaderForHostComponents() throws Exception
    {
        final DummyHostComponentImpl comp = new DummyHostComponentImpl(TestCase.class.getName());
        File plugin = new PluginJarBuilder("ccltest")
                .addResource("atlassian-plugin.xml",
                        "<atlassian-plugin key=\"ccltest\" pluginsVersion=\"2\">\n" +
                        "    <plugin-info>\n" +
                        "        <version>1.0</version>\n" +
                        "    </plugin-info>\n" +
                        "    <component key=\"foo\" class=\"my.FooImpl\" />\n" +
                        "</atlassian-plugin>")
                .addJava("my.Foo", "package my;public interface Foo {}")
                .addJava("my.FooImpl", "package my;import com.atlassian.plugin.osgi.DummyHostComponent;" +
                        "public class FooImpl implements Foo {public FooImpl(DummyHostComponent comp) throws Exception { comp.evaluate(); }}")
                .build();
        HostComponentProvider prov = new HostComponentProvider()
        {
            public void provide(ComponentRegistrar registrar)
            {
                registrar.register(DummyHostComponent.class).forInstance(comp).withName("hostComp");
            }
        };

        initPluginManager(prov);
        pluginManager.installPlugin(new JarPluginArtifact(new DeploymentUnit(plugin)));

        assertNotNull(comp.cl);
        assertNotNull(comp.testClass);
        assertTrue(comp.testClass == TestCase.class);

    }

    public void testCorrectContextClassLoaderForHostComponentsUsePluginStrategy() throws Exception
    {
        final DummyHostComponentImpl comp = new DummyHostComponentImpl(TestCase.class.getName());
        File plugin = new PluginJarBuilder("ccltest")
                .addResource("atlassian-plugin.xml",
                        "<atlassian-plugin key=\"ccltest\" pluginsVersion=\"2\">\n" +
                        "    <plugin-info>\n" +
                        "        <version>1.0</version>\n" +
                        "    </plugin-info>\n" +
                        "    <component key=\"foo\" class=\"my.FooImpl\" />\n" +
                        "</atlassian-plugin>")
                .addJava("my.Foo", "package my;public interface Foo {}")
                .addJava("my.FooImpl", "package my;import com.atlassian.plugin.osgi.DummyHostComponent;" +
                        "public class FooImpl implements Foo {public FooImpl(DummyHostComponent comp) throws Exception { comp.evaluate(); }}")
                .build();
        HostComponentProvider prov = new HostComponentProvider()
        {
            public void provide(ComponentRegistrar registrar)
            {
                registrar.register(DummyHostComponent.class).forInstance(comp).withName("hostComp").withContextClassLoaderStrategy(ContextClassLoaderStrategy.USE_PLUGIN);
            }
        };

        initPluginManager(prov);
        pluginManager.installPlugin(new JarPluginArtifact(new DeploymentUnit(plugin)));

        assertNotNull(comp.cl);
        assertNull(comp.testClass);
    }

    public void testCorrectContextClassLoaderForHostComponentsUsePluginStrategyLoadingLocalClass() throws Exception
    {
        final DummyHostComponentImpl comp = new DummyHostComponentImpl("my.Foo");
        File plugin = new PluginJarBuilder("ccltest")
                .addResource("atlassian-plugin.xml",
                        "<atlassian-plugin key=\"ccltest\" pluginsVersion=\"2\">\n" +
                        "    <plugin-info>\n" +
                        "        <version>1.0</version>\n" +
                        "    </plugin-info>\n" +
                        "    <component key=\"foo\" class=\"my.FooImpl\" />\n" +
                        "</atlassian-plugin>")
                .addJava("my.Foo", "package my;public interface Foo {}")
                .addJava("my.FooImpl", "package my;import com.atlassian.plugin.osgi.DummyHostComponent;" +
                        "public class FooImpl implements Foo {public FooImpl(DummyHostComponent comp) throws Exception { comp.evaluate(); }}")
                .build();
        HostComponentProvider prov = new HostComponentProvider()
        {
            public void provide(ComponentRegistrar registrar)
            {
                registrar.register(DummyHostComponent.class).forInstance(comp).withName("hostComp").withContextClassLoaderStrategy(ContextClassLoaderStrategy.USE_PLUGIN);
            }
        };

        initPluginManager(prov);
        pluginManager.installPlugin(new JarPluginArtifact(new DeploymentUnit(plugin)));

        assertNotNull(comp.cl);
        assertNotNull(comp.testClass);
    }

    public void testCorrectContextClassLoaderForHostComponentsUseHostStrategy() throws Exception
    {
        final DummyHostComponentImpl comp = new DummyHostComponentImpl(TestCase.class.getName());
        File plugin = new PluginJarBuilder("ccltest")
                .addResource("atlassian-plugin.xml",
                        "<atlassian-plugin key=\"ccltest\" pluginsVersion=\"2\">\n" +
                        "    <plugin-info>\n" +
                        "        <version>1.0</version>\n" +
                        "    </plugin-info>\n" +
                        "    <component key=\"foo\" class=\"my.FooImpl\" />\n" +
                        "</atlassian-plugin>")
                .addJava("my.Foo", "package my;public interface Foo {}")
                .addJava("my.FooImpl", "package my;import com.atlassian.plugin.osgi.DummyHostComponent;" +
                        "public class FooImpl implements Foo {public FooImpl(DummyHostComponent comp) throws Exception { comp.evaluate(); }}")
                .build();
        HostComponentProvider prov = new HostComponentProvider()
        {
            public void provide(ComponentRegistrar registrar)
            {
                registrar.register(DummyHostComponent.class).forInstance(comp).withName("hostComp").withContextClassLoaderStrategy(ContextClassLoaderStrategy.USE_HOST);
            }
        };

        initPluginManager(prov);
        pluginManager.installPlugin(new JarPluginArtifact(new DeploymentUnit(plugin)));

        assertNotNull(comp.cl);
        assertNotNull(comp.testClass);
        assertTrue(comp.testClass == TestCase.class);
    }

    public static class DummyHostComponentImpl implements DummyHostComponent
    {
        public ClassLoader cl;
        public Class testClass;
        private String classToLoad;

        public DummyHostComponentImpl(String classToLoad)
        {
            this.classToLoad = classToLoad;
        }

        public void evaluate()
        {
            cl = Thread.currentThread().getContextClassLoader();
            try
            {
                testClass = cl.loadClass(classToLoad);
            } catch (ClassNotFoundException ex) {}
        }
    }
}
