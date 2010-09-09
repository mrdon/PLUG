package com.atlassian.plugin.osgi;

import com.atlassian.plugin.DefaultModuleDescriptorFactory;
import com.atlassian.plugin.JarPluginArtifact;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginState;
import com.atlassian.plugin.hostcontainer.DefaultHostContainer;
import com.atlassian.plugin.impl.UnloadablePlugin;
import com.atlassian.plugin.osgi.external.SingleModuleDescriptorFactory;
import com.atlassian.plugin.osgi.factory.OsgiPlugin;
import com.atlassian.plugin.osgi.hostcomponents.ComponentRegistrar;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentProvider;
import com.atlassian.plugin.servlet.DefaultServletModuleManager;
import com.atlassian.plugin.servlet.ServletModuleManager;
import com.atlassian.plugin.servlet.descriptors.ServletModuleDescriptor;
import com.atlassian.plugin.test.PluginJarBuilder;
import com.atlassian.plugin.util.PluginUtils;
import com.atlassian.plugin.util.WaitUntil;
import com.google.common.collect.ImmutableMap;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestPluginInstall extends PluginInContainerTestBase
{
    private static final String EMPTY_SPRING_CONFIG = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<beans xmlns=\"http://www.springframework.org/schema/beans\"\n"
            + "       xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
            + "       xmlns:osgi=\"http://www.springframework.org/schema/osgi\"\n"
            + "       xsi:schemaLocation=\"http://www.springframework.org/schema/beans\n"
            + "           http://www.springframework.org/schema/beans/spring-beans-2.5.xsd\n"
            + "           http://www.springframework.org/schema/osgi\n"
            + "           http://www.springframework.org/schema/osgi/spring-osgi.xsd\"\n"
            + ">\n"
            + "</beans>";

    public void testUpgradeOfBundledPlugin() throws Exception
    {
        final DefaultModuleDescriptorFactory factory = new DefaultModuleDescriptorFactory(hostContainer);
        factory.addModuleDescriptor("object", ObjectModuleDescriptor.class);

        final File pluginJar = new PluginJarBuilder("testUpgradeOfBundledPlugin")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test' key='test.bundled.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <object key='obj' class='my.Foo'/>",
                        "</atlassian-plugin>")
                .addFormattedJava("my.Foo",
                        "package my;",
                        "public class Foo {}")
                .build();
        initBundlingPluginManager(factory, pluginJar);
        assertEquals(1, pluginManager.getEnabledPlugins().size());
        assertEquals("Test", pluginManager.getPlugin("test.bundled.plugin").getName());
        assertEquals("my.Foo", pluginManager.getPlugin("test.bundled.plugin").getModuleDescriptor("obj").getModule().getClass().getName());

        final File pluginJar2 = new PluginJarBuilder("testUpgradeOfBundledPlugin")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test' key='test.bundled.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <object key='obj' class='my.Bar'/>",
                        "</atlassian-plugin>")
                .addFormattedJava("my.Bar",
                        "package my;",
                        "public class Bar {}")
                .build();

        pluginManager.installPlugin(new JarPluginArtifact(pluginJar2));

        assertEquals(1, pluginManager.getEnabledPlugins().size());
        assertEquals("Test", pluginManager.getPlugin("test.bundled.plugin").getName());
        assertEquals("my.Bar", pluginManager.getPlugin("test.bundled.plugin").getModuleDescriptor("obj").getModule().getClass().getName());

    }

    public void testUpgradeOfBadPlugin() throws Exception
    {
        new PluginJarBuilder("testUpgradeOfBundledPlugin-old")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test' key='test.bundled.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <component key='obj' class='my.Foo'/>",
                        "</atlassian-plugin>")
                .addFormattedJava("my.Foo",
                        "package my;",
                        "public class Foo {",
                        "  public Foo() { throw new RuntimeException('bad plugin');}",
                        "}")
                .build(pluginsDir);
        new PluginJarBuilder("testUpgradeOfBundledPlugin-new")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test' key='test.bundled.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>2.0</version>",
                        "    </plugin-info>",
                        "    <component key='obj' class='my.Foo'/>",
                        "</atlassian-plugin>")
                .addFormattedJava("my.Foo",
                        "package my;",
                        "public class Foo {",
                        "  public Foo() {}",
                        "}")
                .build(pluginsDir);
        initPluginManager();
        assertEquals(1, pluginManager.getEnabledPlugins().size());
        assertEquals("Test", pluginManager.getPlugin("test.bundled.plugin").getName());
        assertEquals("2.0", pluginManager.getPlugin("test.bundled.plugin").getPluginInformation().getVersion());
    }

    public void testUpgradeWithNewComponentImports() throws Exception
    {
        final DefaultModuleDescriptorFactory factory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
        factory.addModuleDescriptor("dummy", DummyModuleDescriptor.class);
        initPluginManager(new HostComponentProvider()
        {
            public void provide(final ComponentRegistrar registrar)
            {
                registrar.register(SomeInterface.class).forInstance(new SomeInterface()
                {
                });
                registrar.register(AnotherInterface.class).forInstance(new AnotherInterface()
                {
                });
            }
        }, factory);

        final File pluginJar = new PluginJarBuilder("first")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test' key='test.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <component-import key='comp1' interface='com.atlassian.plugin.osgi.SomeInterface' />",
                        "    <dummy key='dum1'/>", "</atlassian-plugin>")
                .build();
        final File pluginJar2 = new PluginJarBuilder("second")
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
        assertEquals(2, pluginManager.getPlugin("test.plugin").getModuleDescriptors().size());
        pluginManager.installPlugin(new JarPluginArtifact(pluginJar2));
        assertEquals(1, pluginManager.getEnabledPlugins().size());
        assertEquals(4, pluginManager.getPlugin("test.plugin").getModuleDescriptors().size());
        assertEquals("Test 2", pluginManager.getPlugin("test.plugin").getName());
    }

    public void testInstallWithClassConstructorReferencingHostClassWithHostComponent() throws Exception
    {
        final DefaultModuleDescriptorFactory factory = new DefaultModuleDescriptorFactory(hostContainer);
        factory.addModuleDescriptor("object", ObjectModuleDescriptor.class);
        initPluginManager(new HostComponentProvider()
        {
            public void provide(final ComponentRegistrar registrar)
            {
                registrar.register(SomeInterface.class).forInstance(new SomeInterface()
                {
                });
            }
        }, factory);

        final File pluginJar = new PluginJarBuilder("testUpgradeOfBundledPlugin")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test' key='hostClass' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <object key='hostClass' class='com.atlassian.plugin.osgi.HostClassUsingHostComponentConstructor'/>",
                        "</atlassian-plugin>")
                .build();

        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        assertEquals(1, pluginManager.getEnabledPlugins().size());
        assertEquals("Test", pluginManager.getPlugin("hostClass").getName());
        assertEquals(1, pluginManager.getPlugin("hostClass").getModuleDescriptors().size());
        
        HostClassUsingHostComponentConstructor module = (HostClassUsingHostComponentConstructor) pluginManager.getPlugin("hostClass").getModuleDescriptor("hostClass").getModule();
        assertNotNull(module);
    }

    public void testInstallWithClassSetterReferencingHostClassWithHostComponent() throws Exception
    {
        final DefaultModuleDescriptorFactory factory = new DefaultModuleDescriptorFactory(hostContainer);
        factory.addModuleDescriptor("object", ObjectModuleDescriptor.class);

        initPluginManager(new HostComponentProvider()
        {
            public void provide(final ComponentRegistrar registrar)
            {
                registrar.register(SomeInterface.class).forInstance(new SomeInterface()
                {
                });
            }
        }, factory);

        final File pluginJar = new PluginJarBuilder("testUpgradeOfBundledPlugin")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test' key='hostClass' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <object key='hostClass' class='com.atlassian.plugin.osgi.HostClassUsingHostComponentSetter'/>",
                        "</atlassian-plugin>")
                .build();

        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        assertEquals(1, pluginManager.getEnabledPlugins().size());
        assertEquals("Test", pluginManager.getPlugin("hostClass").getName());
        assertEquals(1, pluginManager.getPlugin("hostClass").getModuleDescriptors().size());

        HostClassUsingHostComponentSetter module = (HostClassUsingHostComponentSetter) pluginManager.getPlugin("hostClass").getModuleDescriptor("hostClass").getModule();
        assertNotNull(module);
        assertNotNull(module.getSomeInterface());
    }

    /* Enable for manual memory leak profiling
    public void testNoMemoryLeak() throws Exception
    {
        DefaultModuleDescriptorFactory factory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
        factory.addModuleDescriptor("dummy", DummyModuleDescriptor.class);
        for (int x=0; x<100; x++)
        {
            pluginEventManager = new DefaultPluginEventManager();
            initPluginManager(new HostComponentProvider(){
                public void provide(ComponentRegistrar registrar)
                {
                    registrar.register(SomeInterface.class).forInstance(new SomeInterface(){});
                    registrar.register(AnotherInterface.class).forInstance(new AnotherInterface(){});
                }
            }, factory);
            pluginManager.shutdown();

        }
        System.out.println("Gentlement, start your profilers!");
        System.in.read();

    }
    */


    public void testUpgradeWithNoAutoDisable() throws Exception
    {
        DefaultModuleDescriptorFactory factory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
        factory.addModuleDescriptor("dummy", DummyModuleDescriptor.class);
        initPluginManager(new HostComponentProvider(){
            public void provide(ComponentRegistrar registrar)
            {
                registrar.register(SomeInterface.class).forInstance(new SomeInterface(){});
                registrar.register(AnotherInterface.class).forInstance(new AnotherInterface(){});
            }
        }, factory);

        File pluginJar = new PluginJarBuilder("first")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test' key='test.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <component-import key='comp1' interface='com.atlassian.plugin.osgi.SomeInterface' />",
                        "    <dummy key='dum1'/>",
                        "</atlassian-plugin>")
                .build();
        final File pluginJar2 = new PluginJarBuilder("second")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test 2' key='test.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <component-import key='comp1' interface='com.atlassian.plugin.osgi.SomeInterface' />",
                        "    <dummy key='dum1'/>",
                        "    <dummy key='dum2'/>",
                        "</atlassian-plugin>")
                .build();

        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        assertTrue(pluginManager.isPluginEnabled("test.plugin"));

        final Lock lock = new ReentrantLock();
        Thread upgradeThread = new Thread()
        {
            public void run()
            {
                lock.lock();
                pluginManager.installPlugin(new JarPluginArtifact(pluginJar2));
                lock.unlock();
            }
        };

        Thread isEnabledThread = new Thread()
        {
            public void run()
            {
                try
                {
                    while (!lock.tryLock(10, TimeUnit.SECONDS))
                        pluginManager.isPluginEnabled("test.plugin");
                    }
                catch (InterruptedException e)
                {
                    fail();
                }
            }
        };
        upgradeThread.start();
        isEnabledThread.start();

        upgradeThread.join(10000);

        assertTrue(pluginManager.isPluginEnabled("test.plugin"));
    }

    public void testUpgradeTestingForCachedXml() throws Exception
    {
        final DefaultModuleDescriptorFactory factory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
        factory.addModuleDescriptor("dummy", DummyModuleDescriptor.class);
        initPluginManager(new HostComponentProvider()
        {
            public void provide(final ComponentRegistrar registrar)
            {
                registrar.register(SomeInterface.class).forInstance(new SomeInterface()
                {});
                registrar.register(AnotherInterface.class).forInstance(new AnotherInterface()
                {});
            }
        }, factory);

        final File pluginJar = new PluginJarBuilder("first").addFormattedResource("atlassian-plugin.xml",
            "<atlassian-plugin name='Test' key='test.plugin' pluginsVersion='2'>", "    <plugin-info>", "        <version>1.0</version>",
            "    </plugin-info>", "    <component key='comp1' interface='com.atlassian.plugin.osgi.SomeInterface' class='my.ServiceImpl' />",
            "</atlassian-plugin>").addFormattedJava("my.ServiceImpl", "package my;",
            "public class ServiceImpl implements com.atlassian.plugin.osgi.SomeInterface {}").build();
        final File pluginJar2 = new PluginJarBuilder("second").addFormattedResource("atlassian-plugin.xml",
            "<atlassian-plugin name='Test 2' key='test.plugin' pluginsVersion='2'>", "    <plugin-info>", "        <version>1.0</version>",
            "    </plugin-info>", "</atlassian-plugin>").build();

        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        assertEquals(1, pluginManager.getEnabledPlugins().size());
        assertEquals("Test", pluginManager.getPlugin("test.plugin").getName());
        pluginManager.installPlugin(new JarPluginArtifact(pluginJar2));
        assertEquals(1, pluginManager.getEnabledPlugins().size());
        assertEquals("Test 2", pluginManager.getPlugin("test.plugin").getName());
    }

    public void testPluginDependentOnPackageImport() throws Exception
    {
        HostComponentProvider prov = new HostComponentProvider()
        {
            public void provide(final ComponentRegistrar registrar)
            {
                registrar.register(ServletConfig.class).forInstance(new HttpServlet() {});
            }
        };
        File servletJar =  new PluginJarBuilder("first")
                .addFormattedResource("META-INF/MANIFEST.MF",
                        "Export-Package: javax.servlet.http;version='4.0.0',javax.servlet;version='4.0.0'",
                        "Import-Package: javax.servlet.http;version='4.0.0',javax.servlet;version='4.0.0'",
                        "Bundle-SymbolicName: first",
                        "Bundle-Version: 4.0.0",
                        "Manifest-Version: 1.0",
                        "")
                .addFormattedJava("javax.servlet.Servlet",
                        "package javax.servlet;",
                        "public interface Servlet {}")
                .addFormattedJava("javax.servlet.http.HttpServlet",
                        "package javax.servlet.http;",
                        "public abstract class HttpServlet implements javax.servlet.Servlet{}")
                .build();

        File pluginJar = new PluginJarBuilder("asecond")
                .addFormattedResource("atlassian-plugin.xml",
                    "<atlassian-plugin name='Test' key='second' pluginsVersion='2'>",
                    "    <plugin-info>",
                    "        <version>1.0</version>",
                    "        <bundle-instructions><Import-Package>javax.servlet.http;version='[2.3,2.3]',javax.servlet;version='[2.3,2.3]',*</Import-Package></bundle-instructions>",
                    "    </plugin-info>",
                    "</atlassian-plugin>")
                .addFormattedJava("second.MyImpl",
                        "package second;",
                        "public class MyImpl {",
                        "    public MyImpl(javax.servlet.ServletConfig config) {",
                        "    }",
                        "}")
                .build();

        initPluginManager(prov);
        pluginManager.installPlugin(new JarPluginArtifact(servletJar));
        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        WaitUntil.invoke(new BasicWaitCondition()
        {
            public boolean isFinished()
            {
                return pluginManager.getEnabledPlugins().size() == 2;
            }
        });

        assertEquals(2, pluginManager.getEnabledPlugins().size());
        assertNotNull(pluginManager.getPlugin("first-4.0.0"));
        assertNotNull(pluginManager.getPlugin("second"));
    }

    public void testPluginWithHostComponentUsingOldPackageImport() throws Exception
    {
        final PluginJarBuilder firstBuilder = new PluginJarBuilder("oldpkgfirst");
        firstBuilder
                .addFormattedResource("atlassian-plugin.xml",
                    "<atlassian-plugin name='Test' key='first' pluginsVersion='2'>",
                    "    <plugin-info>",
                    "        <version>1.0</version>",
                    "        <bundle-instructions>",
                    "           <Export-Package>first</Export-Package>",
                    "        </bundle-instructions>",
                    "    </plugin-info>",
                    "    <servlet key='foo' class='second.MyServlet'>",
                    "       <url-pattern>/foo</url-pattern>",
                    "    </servlet>",
                    "</atlassian-plugin>")
                .addFormattedJava("first.MyInterface",
                        "package first;",
                        "public interface MyInterface {}")
                .build(pluginsDir);

        new PluginJarBuilder("oldpkgsecond", firstBuilder.getClassLoader())
                .addPluginInformation("second", "Some name", "1.0")
                .addFormattedJava("second.MyImpl",
                        "package second;",
                        "public class MyImpl implements first.MyInterface {}")
                .build(pluginsDir);

        initPluginManager();

        assertEquals(2, pluginManager.getEnabledPlugins().size());
        assertNotNull(pluginManager.getPlugin("first"));
        assertNotNull(pluginManager.getPlugin("second"));
    }

    public void testPluginWithServletDependentOnPackageImport() throws Exception
    {
        final PluginJarBuilder firstBuilder = new PluginJarBuilder("first");
        firstBuilder
                .addPluginInformation("first", "Some name", "1.0")
                .addFormattedJava("first.MyInterface",
                        "package first;",
                        "public interface MyInterface {}")
                .addFormattedResource("META-INF/MANIFEST.MF",
                    "Manifest-Version: 1.0",
                    "Bundle-SymbolicName: first",
                    "Bundle-Version: 1.0",
                    "Export-Package: first",
                    "")
                .build(pluginsDir);

        new PluginJarBuilder("asecond", firstBuilder.getClassLoader())
                .addFormattedResource("atlassian-plugin.xml",
                    "<atlassian-plugin name='Test' key='asecond' pluginsVersion='2'>",
                    "    <plugin-info>",
                    "        <version>1.0</version>",
                    "    </plugin-info>",
                    "    <servlet key='foo' class='second.MyServlet'>",
                    "       <url-pattern>/foo</url-pattern>",
                    "    </servlet>",
                    "</atlassian-plugin>")
                .addFormattedJava("second.MyServlet",
                    "package second;",
                    "public class MyServlet extends javax.servlet.http.HttpServlet implements first.MyInterface {}")
                .build(pluginsDir);

        initPluginManager(null, new SingleModuleDescriptorFactory(new DefaultHostContainer(), "servlet", StubServletModuleDescriptor.class));

        assertEquals(2, pluginManager.getEnabledPlugins().size());
        assertTrue(pluginManager.getPlugin("first").getPluginState() == PluginState.ENABLED);
        assertNotNull(pluginManager.getPlugin("asecond").getPluginState() == PluginState.ENABLED);
    }

    public void testPluginWithServletRefreshedAfterOtherPluginUpgraded() throws Exception
    {
        final PluginJarBuilder firstBuilder = new PluginJarBuilder("first");
        firstBuilder
                .addPluginInformation("first", "Some name", "1.0")
                .addFormattedJava("first.MyInterface",
                        "package first;",
                        "public interface MyInterface {}")
                .addFormattedResource("META-INF/MANIFEST.MF",
                    "Manifest-Version: 1.0",
                    "Bundle-SymbolicName: first",
                    "Bundle-Version: 1.0",
                    "Export-Package: first",
                    "")
                .build(pluginsDir);

        new PluginJarBuilder("asecond", firstBuilder.getClassLoader())
                .addFormattedResource("atlassian-plugin.xml",
                    "<atlassian-plugin name='Test' key='asecond' pluginsVersion='2'>",
                    "    <plugin-info>",
                    "        <version>1.0</version>",
                    "    </plugin-info>",
                    "    <servlet key='foo' class='second.MyServlet'>",
                    "       <url-pattern>/foo</url-pattern>",
                    "    </servlet>",
                    "</atlassian-plugin>")
                .addFormattedJava("second.MyServlet",
                    "package second;",
                    "import com.atlassian.plugin.osgi.Callable2;",
                    "public class MyServlet extends javax.servlet.http.HttpServlet implements first.MyInterface {",
                    "   private Callable2 callable;",
                    "   public MyServlet(Callable2 cal) { this.callable = cal; }",
                    "   public String getServletInfo() {",
                    "       try {return callable.call() + ' bob';} catch (Exception ex) { throw new RuntimeException(ex);}",
                    "   }",
                    "}")
                .build(pluginsDir);

        HostComponentProvider prov = new HostComponentProvider()
        {

            public void provide(ComponentRegistrar registrar)
            {
                registrar.register(Callable2.class).forInstance(new Callable2()
                {
                    public String call()
                    {
                        return "hi";
                    }
                });

            }
        };

        ServletContext ctx = mock(ServletContext.class);
        when(ctx.getInitParameterNames()).thenReturn(Collections.enumeration(Collections.emptyList()));
        ServletConfig servletConfig = mock(ServletConfig.class);
        when(servletConfig.getServletContext()).thenReturn(ctx);

        ServletModuleManager mgr = new DefaultServletModuleManager(pluginEventManager);
        hostContainer = createHostContainer(Collections.<Class<?>, Object>singletonMap(ServletModuleManager.class, mgr));
        initPluginManager(prov, new SingleModuleDescriptorFactory(
                hostContainer,
                "servlet",
                ServletModuleDescriptor.class));

        assertEquals(2, pluginManager.getEnabledPlugins().size());
        assertTrue(pluginManager.getPlugin("first").getPluginState() == PluginState.ENABLED);
        assertNotNull(pluginManager.getPlugin("asecond").getPluginState() == PluginState.ENABLED);
        assertEquals("hi bob", mgr.getServlet("/foo", servletConfig).getServletInfo());



        final File updatedJar = new PluginJarBuilder("first-updated")
                .addPluginInformation("foo", "Some name", "1.0")
                .addFormattedJava("first.MyInterface",
                        "package first;",
                        "public interface MyInterface {}")
                .addFormattedResource("META-INF/MANIFEST.MF",
                    "Manifest-Version: 1.0",
                    "Bundle-SymbolicName: foo",
                    "Bundle-Version: 1.0",
                    "Export-Package: first",
                    "")
                .build();
        pluginManager.installPlugin(new JarPluginArtifact(updatedJar));

        WaitUntil.invoke(new BasicWaitCondition()
        {
            public boolean isFinished()
            {
                return pluginManager.isPluginEnabled("asecond");
            }

        });

        assertEquals("hi bob", mgr.getServlet("/foo", servletConfig).getServletInfo());
    }

    public void testLotsOfHostComponents() throws Exception
    {
        new PluginJarBuilder("first")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test' key='test.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <dummy key='dum1'/>",
                        "</atlassian-plugin>")
                .build(pluginsDir);
        new PluginJarBuilder("second")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test 2' key='test.plugin2' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <dummy key='dum1'/>",
                        "    <dummy key='dum2'/>",
                        "</atlassian-plugin>")
                .build(pluginsDir);

        final DefaultModuleDescriptorFactory factory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
        factory.addModuleDescriptor("dummy", DummyModuleDescriptor.class);
        initPluginManager(new HostComponentProvider()
        {
            public void provide(final ComponentRegistrar registrar)
            {
                for (int x = 0; x < 100; x++)
                {
                    registrar.register(SomeInterface.class).forInstance(new SomeInterface()
                    {}).withName("some" + x);
                    registrar.register(AnotherInterface.class).forInstance(new AnotherInterface()
                    {}).withName("another" + x);
                }
            }
        }, factory);

        assertEquals(2, pluginManager.getEnabledPlugins().size());
    }

    public void testInstallWithManifestNoSpringContextAndComponents() throws Exception
    {
        final BooleanFlag flag = new DefaultBooleanFlag(false);
        new PluginJarBuilder("first")
                .addFormattedResource("atlassian-plugin.xml",
                    "<atlassian-plugin name='Test' key='first' pluginsVersion='2'>",
                    "    <plugin-info>",
                    "        <version>1.0</version>",
                    "    </plugin-info>",
                    "    <component key='foo' class='first.MyClass' interface='first.MyInterface' public='true'/>",
                    "</atlassian-plugin>")
                .addFormattedJava("first.MyInterface",
                        "package first;",
                        "public interface MyInterface {}")
                .addFormattedJava("first.MyClass",
                        "package first;",
                        "public class MyClass implements MyInterface{",
                        "  public MyClass(com.atlassian.plugin.osgi.BooleanFlag bool) { bool.set(true); }",
                        "}")
                .addFormattedResource("META-INF/MANIFEST.MF",
                        "Manifest-Version: 1.0",
                        "Bundle-SymbolicName: foo",
                        "Bundle-Version: 1.0",
                        "Export-Package: first",
                        "")
                .build(pluginsDir);

        initPluginManager(new HostComponentProvider()
        {
            public void provide(ComponentRegistrar registrar)
            {
                registrar.register(BooleanFlag.class).forInstance(flag).withName("bob");
            }
        });

        assertEquals(1, pluginManager.getEnabledPlugins().size());
        assertNotNull(pluginManager.getPlugin("first"));
        assertTrue(flag.get());
    }

    public void testInstallWithManifestNoDescriptorWithSpringXml() throws Exception
    {
        new PluginJarBuilder("nodesc")
                .manifest(new ImmutableMap.Builder<String, String>()
                        .put(OsgiPlugin.ATLASSIAN_PLUGIN_KEY, "nodesc")
                        .put(Constants.BUNDLE_VERSION, "1.0")
                        .build())
                .addResource("META-INF/spring/foo.xml", EMPTY_SPRING_CONFIG)
                .build(pluginsDir);

        initPluginManager();

        assertEquals(1, pluginManager.getEnabledPlugins().size());
        Plugin plugin = pluginManager.getPlugin("nodesc");
        assertNotNull(plugin);
        assertEquals("1.0", plugin.getPluginInformation().getVersion());
        assertEquals("nodesc", plugin.getKey());
        assertEquals(0, plugin.getModuleDescriptors().size());
    }

    public void testInstallWithManifestNoDescriptorWithSpringHeader() throws Exception
    {
        new PluginJarBuilder("nodesc")
                .manifest(new ImmutableMap.Builder<String, String>()
                        .put(OsgiPlugin.ATLASSIAN_PLUGIN_KEY, "nodesc")
                        .put(Constants.BUNDLE_VERSION, "1.0")
                        .put("Spring-Context", "*;timeout:=60")
                        .build())
                .build(pluginsDir);

        initPluginManager();

        assertEquals(1, pluginManager.getEnabledPlugins().size());
        Plugin plugin = pluginManager.getPlugin("nodesc");
        assertNotNull(plugin);
        assertEquals("1.0", plugin.getPluginInformation().getVersion());
        assertEquals("nodesc", plugin.getKey());
        assertEquals(0, plugin.getModuleDescriptors().size());
    }

    public void testInstallWithComponentBeanNameConflictedWithHostComponent() throws Exception
    {
        new PluginJarBuilder("first")
                .addFormattedResource("atlassian-plugin.xml",
                    "<atlassian-plugin name='Test' key='first' pluginsVersion='2'>",
                    "    <plugin-info>",
                    "        <version>1.0</version>",
                    "    </plugin-info>",
                    "    <component key='host_component1' class='first.MyClass' interface='first.MyInterface'/>",
                    "</atlassian-plugin>")
                .addFormattedJava("com.atlassian.plugin.osgi.SomeInterface",
                        "package com.atlassian.plugin.osgi;",
                        "public interface SomeInterface {}")
                .addFormattedJava("first.MyClass",
                        "package first;",
                        "import com.atlassian.plugin.osgi.SomeInterface;",
                        "public class MyClass implements SomeInterface{",
                        "}")
                .build(pluginsDir);

        initPluginManager(new HostComponentProvider()
        {
            public void provide(ComponentRegistrar registrar)
            {
                registrar.register(SomeInterface.class).forInstance(new SomeInterface(){}).withName("host_component1");
            }
        });

        // there is a name conflict therefore this plugin should not have been enabled.
        assertEquals(0, pluginManager.getEnabledPlugins().size());
        assertTrue(pluginManager.getPlugins().toArray()[0] instanceof UnloadablePlugin);

        // the error message has to mention the problematic component name otherwise users won't be able to figure out.
        assertTrue(pluginManager.getPlugins().toArray(new UnloadablePlugin[1])[0].getErrorText().contains("host_component1"));
    }

    public void testInstallWithStrangePath() throws Exception
    {
        File strangeDir = new File(tmpDir, "20%time");
        strangeDir.mkdir();
        File oldTmp = tmpDir;
        try
        {
            tmpDir = strangeDir;
            cacheDir = new File(tmpDir, "felix-cache");
            cacheDir.mkdir();
            pluginsDir = new File(tmpDir, "plugins");
            pluginsDir.mkdir();

            new PluginJarBuilder("strangePath")
                    .addFormattedResource("atlassian-plugin.xml",
                            "<atlassian-plugin name='Test' key='test.plugin' pluginsVersion='2'>",
                            "    <plugin-info>",
                            "        <version>1.0</version>",
                            "    </plugin-info>",
                            "</atlassian-plugin>")
                    .build(pluginsDir);

            final DefaultModuleDescriptorFactory factory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
            factory.addModuleDescriptor("dummy", DummyModuleDescriptor.class);
            initPluginManager(new HostComponentProvider()
            {
                public void provide(final ComponentRegistrar registrar)
                {
                }
            }, factory);

            assertEquals(1, pluginManager.getEnabledPlugins().size());
        }
        finally
        {
            tmpDir = oldTmp;
        }
    }

    public void testInstallWithUnsatisifedDependency() throws Exception
    {
        File plugin = new PluginJarBuilder("unsatisifiedDependency")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test' key='test.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <component-import key='foo' interface='java.util.concurrent.Callable' />",
                        "</atlassian-plugin>")
                .build(pluginsDir);

        long start = System.currentTimeMillis();
        // Set dev mode temporarily
        System.setProperty("atlassian.dev.mode", "true");
        System.setProperty(PluginUtils.ATLASSIAN_PLUGINS_ENABLE_WAIT, "7");
        try
        {
            initPluginManager();

            assertTrue(start + (60 * 1000) > System.currentTimeMillis());
        }
        finally
        {
            // Undo dev mode
            System.setProperty("atlassian.dev.mode", "false");
            System.clearProperty(PluginUtils.ATLASSIAN_PLUGINS_ENABLE_WAIT);
        }
    }

    public void testInstallSimplePluginNoSpring() throws Exception
    {
        File jar = new PluginJarBuilder("strangePath")
                .addPluginInformation("no-spring", "foo", "1.0")
                .build();

        initPluginManager();
        pluginManager.installPlugin(new JarPluginArtifact(jar));

        assertEquals(1, pluginManager.getEnabledPlugins().size());
    }

    public void testInstallSimplePluginWithNoManifest() throws Exception
    {
        File jar = new PluginJarBuilder("strangePath")
                .addPluginInformation("no-spring", "foo", "1.0")
                .buildWithNoManifest();

        initPluginManager();
        pluginManager.installPlugin(new JarPluginArtifact(jar));

        assertEquals(1, pluginManager.getEnabledPlugins().size());
    }

    public void testInstallPluginTakesTooLong() throws Exception
    {
        System.setProperty(PluginUtils.ATLASSIAN_PLUGINS_ENABLE_WAIT, "3");
        try
        {
            final PluginJarBuilder builder = new PluginJarBuilder("first")
                    .addFormattedResource("atlassian-plugin.xml",
                            "<atlassian-plugin name='Test' key='test.plugin' pluginsVersion='2'>",
                            "    <plugin-info>",
                            "        <version>1.0</version>",
                            "    </plugin-info>",
                            "    <component key='svc' class='my.ServiceImpl' public='true'>",
                            "    <interface>my.Service</interface>",
                            "    </component>",
                            "</atlassian-plugin>")
                    .addFormattedJava("my.Service",
                            "package my;",
                            "public interface Service {",
                            "    public Object call() throws Exception;",
                            "}")
                    .addFormattedJava("my.ServiceImpl",
                            "package my;",
                            "public class ServiceImpl implements Service {",
                            "public ServiceImpl(){",
                            "try{",
                            "Thread.sleep(10000);",
                            "}catch(Exception e){}",
                            "}",
                            "    public Object call() throws Exception { ",
                            "   return 'hi';}",
                            "}");
            final File jar = builder.build();
            initPluginManager();

            pluginManager.installPlugin(new JarPluginArtifact(jar));

            assertEquals(0, pluginManager.getEnabledPlugins().size());
        }
        finally
        {
            System.clearProperty(PluginUtils.ATLASSIAN_PLUGINS_ENABLE_WAIT);
        }
    }

    public void testPersistTimeoutSystemProperty() throws Exception
    {
        System.setProperty(PluginUtils.ATLASSIAN_PLUGINS_ENABLE_WAIT, "300");
        try
        {
            final File propertiesFile = new File(cacheDir, ".properties");

            assertFalse(propertiesFile.exists());

            final PluginJarBuilder firstBuilder = new PluginJarBuilder("first");

            final File jar = firstBuilder
                    .addPluginInformation("first", "Some name", "1.0")
                    .addFormattedJava("first.MyInterface",
                            "package first;",
                            "public interface MyInterface {}")
                    .addFormattedResource("META-INF/MANIFEST.MF",
                            "Manifest-Version: 1.0",
                            "Bundle-SymbolicName: first",
                            "Bundle-Version: 1.0",
                            "Export-Package: first",
                            "").build();
            initPluginManager();

            pluginManager.installPlugin(new JarPluginArtifact(jar));

            Bundle bundle = findBundleByName("first");
            Dictionary headers = bundle.getHeaders();
            assertEquals("*;timeout:=300", headers.get("Spring-Context"));

            assertTrue(propertiesFile.exists());

            Properties properties = new Properties();
            properties.load(new FileInputStream(propertiesFile));
            assertEquals("300", properties.getProperty("spring.timeout"));

            final File testFile = new File(new File(cacheDir, "transformed-plugins"), ".test");
            assertTrue(testFile.createNewFile());
            assertTrue(testFile.exists());

            initPluginManager();
            pluginManager.installPlugin(new JarPluginArtifact(jar));

            bundle = findBundleByName("first");
            headers = bundle.getHeaders();
            assertEquals("*;timeout:=300", headers.get("Spring-Context"));

            assertTrue(propertiesFile.exists());
            assertTrue(testFile.exists());
        }
        finally
        {
            System.clearProperty(PluginUtils.ATLASSIAN_PLUGINS_ENABLE_WAIT);
        }
    }

    public void testDeleteTimeoutFileWhenNoSystemPropertySpecified() throws Exception
    {
        System.setProperty(PluginUtils.ATLASSIAN_PLUGINS_ENABLE_WAIT, "300");
        try
        {
            final File propertiesFile = new File(cacheDir, ".properties");

            assertFalse(propertiesFile.exists());

            final PluginJarBuilder firstBuilder = new PluginJarBuilder("first");

            final File jar = firstBuilder
                    .addPluginInformation("first", "Some name", "1.0")
                    .addFormattedJava("first.MyInterface",
                            "package first;",
                            "public interface MyInterface {}")
                    .addFormattedResource("META-INF/MANIFEST.MF",
                            "Manifest-Version: 1.0",
                            "Bundle-SymbolicName: first",
                            "Bundle-Version: 1.0",
                            "Export-Package: first",
                            "Spring-Context: *;create-asynchrously:=false",
                            "").build();

            initPluginManager();
            pluginManager.installPlugin(new JarPluginArtifact(jar));

            assertTrue(propertiesFile.exists());

            System.clearProperty(PluginUtils.ATLASSIAN_PLUGINS_ENABLE_WAIT);
            initPluginManager();
            pluginManager.installPlugin(new JarPluginArtifact(jar));

            assertFalse(propertiesFile.exists());

            Bundle bundle = findBundleByName("first");
            Dictionary headers = bundle.getHeaders();
            assertEquals("*;create-asynchrously:=false", headers.get("Spring-Context"));
        }
        finally
        {
            System.clearProperty(PluginUtils.ATLASSIAN_PLUGINS_ENABLE_WAIT);
        }
    }

    private Bundle findBundleByName(final String name)
    {
        final Bundle[] bundles = osgiContainerManager.getBundles();
        for (int i = 0; i < bundles.length; i++)
        {
            Bundle bundle = bundles[i];
            if (name.equals(bundle.getSymbolicName()))
            {
                return bundle;
            }
        }
        return null;
    }

    public void testPersistTimeoutSystemPropertyUpdateExistingTimeout() throws Exception
    {
        System.setProperty(PluginUtils.ATLASSIAN_PLUGINS_ENABLE_WAIT, "300");
        try
        {
            final File dummySpringXMLFile = File.createTempFile("temp", "account-data-context.xml", new File(System.getProperty("java.io.tmpdir")));
            FileWriter fileWriter = new FileWriter(dummySpringXMLFile);
            fileWriter.write(EMPTY_SPRING_CONFIG);
            fileWriter.close();
            final File propertiesFile = new File(cacheDir, ".properties");

            assertFalse(propertiesFile.exists());

            final PluginJarBuilder firstBuilder = new PluginJarBuilder("first");

            final File jar = firstBuilder
                    .addPluginInformation("first", "Some name", "1.0")
                    .addFormattedJava("first.MyInterface",
                            "package first;",
                            "public interface MyInterface {}")
                    .addFormattedResource("META-INF/MANIFEST.MF",
                            "Manifest-Version: 1.0",
                            "Bundle-SymbolicName: first",
                            "Bundle-Version: 1.0",
                            "Export-Package: first",
                            "Spring-Context: config/account-data-context.xml;create-asynchrously:=false",
                            "")
                    .addFile("config/account-data-context.xml", dummySpringXMLFile)
                    .build();
            initPluginManager();

            pluginManager.installPlugin(new JarPluginArtifact(jar));

            Bundle bundle = findBundleByName("first");
            Dictionary headers = bundle.getHeaders();
            assertEquals("config/account-data-context.xml;create-asynchrously:=false;timeout:=300", headers.get("Spring-Context"));

            assertTrue(propertiesFile.exists());

            Properties properties = new Properties();
            properties.load(new FileInputStream(propertiesFile));
            assertEquals("300", properties.getProperty("spring.timeout"));

            final File testFile = new File(new File(cacheDir, "transformed-plugins"), ".test");
            assertTrue(testFile.createNewFile());
            assertTrue(testFile.exists());

            System.setProperty(PluginUtils.ATLASSIAN_PLUGINS_ENABLE_WAIT, "301");

            initPluginManager();
            pluginManager.installPlugin(new JarPluginArtifact(jar));

            bundle = findBundleByName("first");
            headers = bundle.getHeaders();
            assertEquals("config/account-data-context.xml;create-asynchrously:=false;timeout:=301", headers.get("Spring-Context"));

            assertTrue(propertiesFile.exists());
            properties.load(new FileInputStream(propertiesFile));
            assertEquals("301", properties.getProperty("spring.timeout"));
        }
        finally
        {
            System.clearProperty(PluginUtils.ATLASSIAN_PLUGINS_ENABLE_WAIT);
        }
    }

    public static class Callable3Aware
    {
        private final Callable3 callable;

        public Callable3Aware(Callable3 callable)
        {
            this.callable = callable;
        }

        public String call() throws Exception
        {
            return callable.call();
        }
    }
}
